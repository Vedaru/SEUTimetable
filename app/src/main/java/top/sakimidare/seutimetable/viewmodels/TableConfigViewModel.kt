package top.sakimidare.seutimetable.viewmodels

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import top.sakimidare.seutimetable.data.model.Period
import top.sakimidare.seutimetable.data.model.SemesterConfig
import top.sakimidare.seutimetable.data.model.TableMetadata
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
class TableConfigViewModel(
    private val initialMetadata: TableMetadata?
) : ViewModel() {

    // 标识当前是“编辑旧表”还是“新建课表”
    val isEditMode = initialMetadata != null

    var tableName by mutableStateOf(initialMetadata?.tableName ?: "")
    var weeks by mutableIntStateOf(initialMetadata?.semesterConfig?.weeks?: 16)
    var showWeekend by mutableStateOf(initialMetadata?.semesterConfig?.visibleDays?.contains(DayOfWeek.SATURDAY) ?: false)

    // 💡 关键：确保 periods 是全新的拷贝，不再引用 initialMetadata 的内部对象
    var periods by mutableStateOf(
        initialMetadata?.semesterConfig?.periods?.toList() ?: SemesterConfig.default().periods
    )

    var selectedDateMillis by mutableLongStateOf(
        (initialMetadata?.semesterConfig?.startDate ?: SemesterConfig.default().startDate)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    val selectedLocalDate: LocalDate by derivedStateOf {
        Instant.ofEpochMilli(selectedDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    // --- 逻辑判断 ---

    fun getPeriodErrorType(index: Int): PeriodError? {
        val period = periods[index]
        if (!period.start.isBefore(period.end)) return PeriodError.Internal
        if (index < periods.size - 1 && periods[index + 1].start.isBefore(period.end)) return PeriodError.Sequence
        if (index > 0 && period.start.isBefore(periods[index - 1].end)) return PeriodError.Sequence
        return null
    }

    val isPeriodsValid by derivedStateOf {
        periods.indices.all { getPeriodErrorType(it) == null }
    }

    // --- 数据修改方法 ---

    fun addPeriod() {
        val lastEnd = periods.lastOrNull()?.end ?: LocalTime.of(8, 0)
        periods = periods + Period(
            index = periods.size + 1,
            start = lastEnd.plusMinutes(10),
            end = lastEnd.plusMinutes(55)
        )
    }

    fun removeLastPeriod() {
        if (periods.size > 1) periods = periods.dropLast(1)
    }

    fun updatePeriodTime(index: Int, isStart: Boolean, newTime: LocalTime) {
        val list = periods.toMutableList()
        val old = list[index]
        list[index] = if (isStart) old.copy(start = newTime) else old.copy(end = newTime)
        periods = list
    }

    // --- 核心修正：构建 Metadata ---

    fun getFinalMetadata(): TableMetadata {
        val visibleDays = if (showWeekend) DayOfWeek.entries.toSet()
        else DayOfWeek.entries.filter { it.value <= 5 }.toSet()

        val config = SemesterConfig(
            startDate = selectedLocalDate,
            weeks = weeks,
            visibleDays = visibleDays,
            periods = periods
        )

        val finalName = tableName.trim().ifBlank { "New Timetable" }

        // 💡 重点：如果是新建（initialMetadata == null），TableMetadata 的 ID 必须为 0
        // 这样 Room 才会将其识别为新纪录并分配新 ID
        return if (isEditMode) {
            initialMetadata!!.copy(
                tableName = finalName,
                semesterConfig = config
            )
        } else {
            TableMetadata(
                id = 0L, // 👈 强制为 0，确保是新表
                tableName = finalName,
                semesterConfig = config,
                isCurrent = true
            )
        }
    }
}

// 💡 定义错误枚举，方便 UI 区分提示
enum class PeriodError {
    Internal, // 自身时间倒置
    Sequence  // 与前后节次冲突
}