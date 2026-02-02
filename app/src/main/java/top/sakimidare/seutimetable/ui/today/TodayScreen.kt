package top.sakimidare.seutimetable.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.SemesterConfig
import top.sakimidare.seutimetable.viewmodels.TimetableViewModel

@Composable
fun TodayScreen(
    windowSize: WindowWidthSizeClass,
    viewModel: TimetableViewModel,
    contentPadding: PaddingValues,
){
    val todayCourses by viewModel.todayCourses.collectAsState()
    val activeCourse by viewModel.activeCourse.collectAsState()
    val semesterConfig by viewModel.semesterConfig.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {},
        bottomBar = { Spacer(Modifier.height(contentPadding.calculateBottomPadding())) }
    ) { scaffoldPadding ->
        val finalPadding = PaddingValues(
            top = scaffoldPadding.calculateTopPadding() + 24.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
            start = 16.dp,
            end = 16.dp
        )
        if (windowSize == WindowWidthSizeClass.Expanded) {
            TodayExpandedContent(
                courses = todayCourses,
                activeCourse = activeCourse,
                semesterConfig = semesterConfig,
                currentWeek = currentWeek,
                padding = finalPadding
            )
        } else {
            TodayCompactContent(
                courses = todayCourses,
                activeCourse = activeCourse,
                semesterConfig = semesterConfig,
                currentWeek = currentWeek,
                padding = finalPadding
            )
        }
    }
}

@Composable
fun TodayCompactContent(
    courses: List<Course>,
    activeCourse: Course?,
    semesterConfig: SemesterConfig,
    currentWeek: Int,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TodayHeader(currentWeek)
            Spacer(Modifier.height(12.dp))
        }
        items(courses) { course ->
            CourseCard(
                course = course,
                semesterConfig = semesterConfig,
                isActive = course == activeCourse
            )
        }
    }
}

@Composable
fun TodayExpandedContent(
    courses: List<Course>,
    activeCourse: Course?,
    semesterConfig: SemesterConfig,
    currentWeek: Int,
    padding: PaddingValues
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // 左侧栏：日期和概览
        Column(modifier = Modifier.weight(1f)) {
            TodayHeader(currentWeek)
            if (activeCourse != null) {
                Text(
                    text = "正在上：${activeCourse.name}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        // 右侧栏：课程列表
        LazyColumn(
            modifier = Modifier.weight(1.5f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(courses) { course ->
                CourseCard(
                    course = course,
                    semesterConfig = semesterConfig,
                    isActive = course == activeCourse
                )
            }
        }
    }
}

@Composable
fun TodayHeader(currentWeek: Int) {
    Column {
        Text(
            text = "第 $currentWeek 周 · 星期$currentWeek}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "今日课程",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EmptyTodayView(padding: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Celebration,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(16.dp))
            Text("今天没有课，去放松一下吧", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun CourseCard(
    course: Course,
    modifier: Modifier = Modifier,
    semesterConfig: SemesterConfig,
    isActive: Boolean
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = CardDefaults.shape,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 时间指示器
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = course.getStartTime(semesterConfig)?.toString() ?: "",
                    style = MaterialTheme.typography.titleMedium
                )
                Box(
                    modifier = Modifier
                        .size(1.dp, 20.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Text(
                    text = course.getEndTime(semesterConfig)?.toString() ?: "",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.width(16.dp))

            // 课程信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${course.location} · ${course.teacher}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 状态标签
            if (isActive) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("上课中") },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        labelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}