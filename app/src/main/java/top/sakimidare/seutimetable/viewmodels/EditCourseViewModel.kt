package top.sakimidare.seutimetable.viewmodels
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.TableMetadata
import top.sakimidare.seutimetable.data.model.WeekRule
import top.sakimidare.seutimetable.ui.theme.courseBackgroundColors
import java.time.DayOfWeek

class EditCourseViewModel(
    private val tableMetadata: TableMetadata,
    private val existingCourses: List<Course>,
    private val currentCourseId: Long
) : ViewModel() {

    // 基础状态
    var name by mutableStateOf("")
    var teacher by mutableStateOf("")
    var location by mutableStateOf("")
    var note by mutableStateOf("")
    var dayOfWeek by mutableStateOf(DayOfWeek.MONDAY)
    var startPeriod by mutableIntStateOf(1)
    var duration by mutableIntStateOf(2)
    var weekRule by mutableStateOf<WeekRule>(WeekRule.All)
    var colorIndex by mutableIntStateOf(0)
    var hasManuallyChangedColor by mutableStateOf(false)

    init {
        // 初始化数据
        existingCourses.find { it.id == currentCourseId }?.let { c ->
            name = c.name
            teacher = c.teacher
            location = c.location
            note = c.note
            dayOfWeek = c.dayOfWeek
            startPeriod = c.startPeriod
            duration = c.duration
            weekRule = c.weekRule
            colorIndex = courseBackgroundColors.indexOf(c.color).coerceAtLeast(0)
            hasManuallyChangedColor = true
        }
    }

    // 逻辑：同名同色联动
    fun onNameChange(newName: String) {
        name = newName
        if (!hasManuallyChangedColor && newName.isNotBlank()) {
            if (courseBackgroundColors.isNotEmpty()) {
                colorIndex = (newName.hashCode() and 0x7FFFFFFF) % courseBackgroundColors.size
            }
        }
    }

    // 逻辑：冲突检测
    val conflictingCourses by derivedStateOf {
        val totalWeeks = tableMetadata.semesterConfig.weeks

        existingCourses.filter { it.isConflictingWith(getResultCourse(), totalWeeks) }
    }

    // 更新判断逻辑
    val isTimeConflict by derivedStateOf { conflictingCourses.isNotEmpty() }
    val canSave by derivedStateOf { name.isNotBlank() && !isTimeConflict }

    fun getResultCourse(): Course {
        val totalWeeks = tableMetadata.semesterConfig.weeks
        // 确保 Custom 规则下的周次不越界
        val finalWeekRule = if (weekRule is WeekRule.Custom) {
            val filteredWeeks = (weekRule as WeekRule.Custom).weeks.filter { it <= totalWeeks }.toSet()
            WeekRule.fromWeeks(filteredWeeks, totalWeeks)
        } else {
            weekRule
        }
        return Course(
            id = currentCourseId,
            name = name.trim(),
            teacher = teacher.trim(),
            location = location.trim(),
            dayOfWeek = dayOfWeek,
            startPeriod = startPeriod,
            duration = duration,
            color = courseBackgroundColors[colorIndex],
            weekRule = finalWeekRule,
            note = note.trim(),
            tableId = tableMetadata.id
        )
    }
}
