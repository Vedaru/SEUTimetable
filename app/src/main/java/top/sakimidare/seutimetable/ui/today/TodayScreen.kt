package top.sakimidare.seutimetable.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.sakimidare.seutimetable.R
import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.SemesterConfig
import top.sakimidare.seutimetable.viewmodels.TimetableViewModel
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    windowSize: WindowWidthSizeClass,
    viewModel: TimetableViewModel,
    contentPadding: PaddingValues,
    onNavigateToTimetable: () -> Unit
) {
    val todayCourses by viewModel.todayCourses.collectAsState()
    val activeCourse by viewModel.activeCourse.collectAsState()
    val nextCourse by viewModel.nextCourse.collectAsState()
    val semesterConfig by viewModel.semesterConfig.collectAsState()
    val actualWeek by viewModel.actualCurrentWeek.collectAsState()
    val todayOfWeek by viewModel.todayDayOfWeek.collectAsState()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { /* 置空 */ },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = { Spacer(Modifier.height(contentPadding.calculateBottomPadding())) }
    ) { scaffoldPadding ->
        val finalPadding = PaddingValues(
            top = scaffoldPadding.calculateTopPadding() + 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
            start = 16.dp,
            end = 16.dp
        )

        if (todayCourses.isEmpty()) {
            EmptyTodayView(
                padding = finalPadding,
                onNavigateToTimetable = onNavigateToTimetable
            )
        } else {
            if (windowSize == WindowWidthSizeClass.Expanded) {
                TodayExpandedContent(
                    courses = todayCourses,
                    activeCourse = activeCourse,
                    nextCourse = nextCourse,
                    semesterConfig = semesterConfig,
                    currentWeek = actualWeek,
                    todayOfWeek = todayOfWeek,
                    padding = finalPadding,
                    viewModel = viewModel
                )
            } else {
                TodayCompactContent(
                    courses = todayCourses,
                    nextCourse = nextCourse,
                    semesterConfig = semesterConfig,
                    currentWeek = actualWeek,
                    todayOfWeek = todayOfWeek,
                    padding = finalPadding,
                    viewModel = viewModel
                )
            }
        }
    }
}

// --- 布局组件 ---

@Composable
private fun TodayCompactContent(
    courses: List<Course>,
    nextCourse: Course?,
    semesterConfig: SemesterConfig,
    currentWeek: Int,
    todayOfWeek: DayOfWeek,
    padding: PaddingValues,
    viewModel: TimetableViewModel
) {
    val activePeriodIndex by viewModel.activePeriodIndex.collectAsState() // 💡 必须 collect
    val currentTime by viewModel.currentTime.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TodayHeader(
                currentWeek,
                todayOfWeek
            )
            Spacer(Modifier.height(12.dp))
        }
        items(courses, key = { it.id }) { course ->
            val isCurrentActive = isCourseActive(course, semesterConfig, currentTime)
            val isBreak = isCurrentActive && activePeriodIndex == -1
            val isPassed = isCoursePassed(course, semesterConfig, currentTime)

            CourseCard(
                course = course,
                semesterConfig = semesterConfig,
                isActive = isCurrentActive,
                isNext = course == nextCourse,
                isPassed = isPassed,
                isBreakTime = isBreak
            )
        }
    }
}

@Composable
private fun TodayExpandedContent(
    courses: List<Course>,
    activeCourse: Course?,
    nextCourse: Course?,
    semesterConfig: SemesterConfig,
    currentWeek: Int,
    todayOfWeek: DayOfWeek,
    padding: PaddingValues,
    viewModel: TimetableViewModel
) {
    val activePeriodIndex by viewModel.activePeriodIndex.collectAsState()
    val isInBreak = activeCourse != null && activePeriodIndex == -1
    val currentTime by viewModel.currentTime.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TodayHeader(currentWeek, todayOfWeek)
            Spacer(Modifier.height(24.dp))
            if (activeCourse != null) {
                StatusInfoSection(
                    label = stringResource(if (isInBreak) R.string.break_time else R.string.having_lesson),
                    courseName = activeCourse.name,
                    color = if (isInBreak) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            } else if (nextCourse != null) {
                StatusInfoSection(
                    stringResource(R.string.next_course),
                    nextCourse.name,
                    MaterialTheme.colorScheme.secondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1.5f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(courses, key = { it.id }) { course ->
                val isCurrentActive = isCourseActive(course, semesterConfig, currentTime)
                val isBreak = isCurrentActive && activePeriodIndex == -1
                val isPassed = isCoursePassed(course, semesterConfig, currentTime)
                CourseCard(
                    course = course,
                    semesterConfig = semesterConfig,
                    isActive = isCurrentActive,
                    isNext = course == nextCourse,
                    isPassed = isPassed,
                    isBreakTime = isBreak
                )
            }
        }
    }
}

// --- 核心子组件 ---

@Composable
private fun CourseCard(
    course: Course,
    semesterConfig: SemesterConfig,
    isActive: Boolean,
    isNext: Boolean,
    isPassed: Boolean,
    isBreakTime: Boolean
) {
    val alpha = if (isPassed) 0.5f else 1f
    val containerColor = when {
        isPassed -> MaterialTheme.colorScheme.surface
        isActive && isBreakTime -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
        isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isActive && isBreakTime -> MaterialTheme.colorScheme.secondary
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = containerColor
        ),
        border = if (isActive) CardDefaults.outlinedCardBorder(true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
        ) else CardDefaults.outlinedCardBorder(true)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .alpha(alpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 时间轴部分
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp)
            ) {
                Text(
                    text = course.getStartTime(semesterConfig)?.toString() ?: "--",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(1.dp, 12.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Text(
                    text = course.getEndTime(semesterConfig)?.toString() ?: "--",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(16.dp))

            // 课程详情部分
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold
                )
                if (course.teacher.isNotBlank() || course.location.isNotBlank()) {
                    Text(
                        text = (if (course.location.isBlank()) "" else (course.location + " · ")) + stringResource(
                            R.string.section_format,
                            course.startPeriod,
                            course.startPeriod + course.duration - 1
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 状态指示
            when {
                isActive -> SuggestionChip(
                    onClick = {},
                    label = {
                        Text(stringResource(if (isBreakTime) R.string.break_time else R.string.having_lesson))
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (isBreakTime) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        labelColor = if (isBreakTime) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                    )
                )

                isNext -> SuggestionChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.upcoming)) }
                )
            }
        }
    }
}

@Composable
private fun TodayHeader(currentWeek: Int, currentDayOfWeek: DayOfWeek) {
    val dayDisplayName = currentDayOfWeek.getDisplayName(
        java.time.format.TextStyle.FULL,
        java.util.Locale.getDefault()
    )
    Column {
        Text(
            text = stringResource(R.string.current_week, currentWeek) + " · $dayDisplayName",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = stringResource(R.string.courses_today),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusInfoSection(label: String, courseName: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        Text(
            courseName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyTodayView(
    padding: PaddingValues,
    onNavigateToTimetable: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Celebration,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.no_courses_today),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onNavigateToTimetable
            ) {
                Text(stringResource(R.string.nav_go_to_timetable))
            }
        }
    }
}

// --- 辅助方法 ---

private fun isCourseActive(
    course: Course,
    semesterConfig: SemesterConfig,
    currentTime: java.time.LocalTime
): Boolean {
    val start = course.getStartTime(semesterConfig) ?: return false
    val end = course.getEndTime(semesterConfig) ?: return false
    // 只要当前时间在整门连堂课的“大开始”和“大结束”之间，就是 Active
    return !currentTime.isBefore(start) && !currentTime.isAfter(end)
}

private fun isCoursePassed(
    course: Course,
    config: SemesterConfig,
    currentTime: java.time.LocalTime
): Boolean {
    val end = course.getEndTime(config) ?: return false
    // 严格晚于整门课的结束时间才算“已结束”
    return currentTime.isAfter(end)
}