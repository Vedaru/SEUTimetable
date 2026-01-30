package top.sakimidare.seutimetable.ui.timetable.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.WeekRule
import java.time.DayOfWeek

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
fun CourseBlockPreview(){
    CourseBlock(
        modifier = Modifier,
        course = mockCourse
    )
}

@Composable
fun CourseBlock(
    modifier: Modifier = Modifier,
    course: Course,
    onClick: (Course) -> Unit = {}
) {
    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(course.color)
            .clickable { onClick(course) }
            .padding(8.dp),
    ) {
        // 课程名称 - 加粗突出
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = course.name,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // 老师名字 - 较小字体
        if (course.teacher.isNotBlank()) {
            Text(
                text = course.teacher,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }

        // 上课地点 - 较小字体
        if (course.location.isNotBlank()) {
            Text(
                text = "@" + course.location,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}