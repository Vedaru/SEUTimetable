package top.sakimidare.seutimetable.ui.timetable.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.SemesterConfig
import top.sakimidare.seutimetable.data.model.TableMetadata
import top.sakimidare.seutimetable.data.model.WeekRule
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.util.Locale

private val mockCourse = Course(
    id = 0,
    tableId = 0,
    name = "测试课程",
    teacher = "老师",
    location = "地点",
    dayOfWeek = DayOfWeek.MONDAY,
    startPeriod = 1,
    duration = 2,
    color = Color.Red,
    weekRule = WeekRule.All,
    note = "备注"
)

@Preview
@Composable
fun TimetableGridPreview() {
    TimetableGrid(
        tableMetadata = TableMetadata(
            id = 0L,
            tableName = "测试课表",
            semesterConfig = SemesterConfig.default(),
            isCurrent = true
        ),
        currentWeek = 2,
        courses = listOf(mockCourse),
        onCourseClick = {}
    )

}
@Composable
fun TimetableGrid(
    tableMetadata: TableMetadata,
    courses: List<Course>,
    currentWeek: Int, // 💡 新增：当前显示的周次
    onCourseClick: (Course) -> Unit,
    activePeriodIndex: Int = -1,
    minPeriodHeight: Dp = 60.dp,
    timeLabelWidth: Dp = 40.dp, // 💡 统一宽度
    titleHeight: Dp = 48.dp     // 💡 统一高度
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val config = tableMetadata.semesterConfig
    val locale = Locale.getDefault()

    // 提取排序后的可见天数
    val sortedVisibleDays = remember(config.visibleDays) {
        config.visibleDays.sortedBy { it.value }
    }

    // 1. 实时时间状态（保持现状）
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(60000)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val availableHeight = maxHeight
        // 注意：如果外层有 Scroll，maxHeight 可能是 Infinity，这里处理一下
        val gridHeight = if (availableHeight.isFinite) availableHeight - titleHeight else 600.dp
        val periodHeight = maxOf(minPeriodHeight, gridHeight / config.periods.size)

        // 3. 计算指示线偏移
        val timeLineOffset = remember(currentTime, config.periods, periodHeight) {
            calculateTimeLineOffset(currentTime, config.periods, periodHeight)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // --- 💡 星期标题栏：现在正确使用了参数 ---
            TimetableHeader(
                currentWeek = currentWeek,
                startDate = config.startDate,
                sortedVisibleDays = sortedVisibleDays,
                timeLabelWidth = timeLabelWidth,
                titleHeight = titleHeight,
                locale = locale
            )

            // --- 核心网格区 ---
            Box(modifier = Modifier.fillMaxWidth()) {
                // A. 底层网格
                Column {
                    config.periods.forEachIndexed { index, period ->
                        val isActive = index == activePeriodIndex
                        Row(modifier = Modifier.height(periodHeight).fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .width(timeLabelWidth)
                                    .fillMaxHeight()
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                PeriodLabel(period = period, isActive = isActive)
                            }
                            sortedVisibleDays.forEach { _ ->
                                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, gridColor))
                            }
                        }
                    }
                }

                // B. 中层时间指示线
                timeLineOffset?.let { yOffset ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = timeLabelWidth)
                            .offset(y = yOffset)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.primary)
                            .zIndex(2f)
                    )
                }

                // C. 顶层课程卡片
                Row(modifier = Modifier.fillMaxWidth().padding(start = timeLabelWidth)) {
                    sortedVisibleDays.forEach { day ->
                        Box(modifier = Modifier.weight(1f).height(periodHeight * config.periods.size)) {
                            courses.filter { it.dayOfWeek == day }.forEach { course ->
                                val topOffset = periodHeight * (course.startPeriod - 1)
                                CourseBlock(
                                    course = course,
                                    modifier = Modifier
                                        .padding(1.dp)
                                        .fillMaxWidth()
                                        .offset(y = topOffset)
                                        .height(periodHeight * course.duration)
                                        .zIndex(1f),
                                    onClick = onCourseClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 💡 提取工具函数：计算指示线偏移
 */
private fun calculateTimeLineOffset(
    currentTime: LocalTime,
    periods: List<top.sakimidare.seutimetable.data.model.Period>,
    periodHeight: Dp
): Dp? {
    if (periods.isEmpty()) return null
    val first = periods.first()
    val last = periods.last()

    if (currentTime.isBefore(first.start) || currentTime.isAfter(last.end)) return null

    for (i in periods.indices) {
        val p = periods[i]
        if (!currentTime.isBefore(p.start) && !currentTime.isAfter(p.end)) {
            val total = Duration.between(p.start, p.end).toMinutes()
            val passed = Duration.between(p.start, currentTime).toMinutes()
            return (i + passed.toFloat() / total).dp * periodHeight.value
        } else if (i < periods.size - 1) {
            val nextP = periods[i + 1]
            if (currentTime.isAfter(p.end) && currentTime.isBefore(nextP.start)) {
                return (i + 1).dp * periodHeight.value
            }
        }
    }
    return null
}