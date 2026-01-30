package top.sakimidare.seutimetable.ui.timetable.view.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.sakimidare.seutimetable.R
import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.WeekRule
import top.sakimidare.seutimetable.ui.timetable.utils.toDisplayText
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    dragHandle: @Composable () -> Unit,
    course: Course,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val configuration = LocalConfiguration.current

    val isWideScreen = configuration.screenWidthDp > 600 // 阈值通常设为 600dp
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        // 调整容器颜色
        containerColor = MaterialTheme.colorScheme.surface,
        // 这里的 dragHandle 就是顶部那根小横条
        dragHandle = dragHandle,
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.verticalScroll(scrollState)
        ) {
            CourseDetailContent(
                course = course,
                isWideScreen = isWideScreen,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

private val mockCourse = Course(
    id = 0,
    tableId = 0,
    name = "123456789123456789123456789123456789123456789",
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
fun CourseDetailContentPreview() {
    CourseDetailContent(
        course = mockCourse,
        onEdit = {},
        onDelete = {}
    )
}


@Composable
fun CourseDetailContent(
    course: Course,
    isWideScreen: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.background(
            MaterialTheme.colorScheme.surface
        )
    ) {
        Heading(course.name)

        if (isWideScreen) {
            // ⭐ 大屏：左右分栏
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // 左侧：信息展示
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoSection(course)
                }
                // 右侧：操作按钮
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ActionButtons(
                        horizontally = false,
                        onEdit = onEdit,
                        onDelete = onDelete
                    ) // 这里的按钮可以堆叠显示
                }
            }
        } else {
            // 竖屏：保持原有的 Column
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                InfoSection(course)
                Spacer(Modifier.height(24.dp))
                ActionButtons(
                    horizontally = true,
                    onEdit = onEdit,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
fun Heading(
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        )
    }
}
@Composable
fun InfoSection(
    course: Course
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {

        // 第一组：地点与老师 (保持原样)
        if (course.location.isNotBlank() || course.teacher.isNotBlank()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconInfoRow(
                    icon = Icons.Default.Place,
                    title = course.location.ifBlank { stringResource(R.string.location_not_specified) },
                    subtitle = course.teacher.ifBlank { stringResource(R.string.teacher_not_specified) },
                    color = course.color
                )
            }
        }

        // 备注 (保持原样)
        if (course.note.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = course.note,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = TextUnit.Unspecified
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // --- 核心修改点：替换掉原本的 when(rule) 手动拼接 ---

        // 1. 调用统一的本地化扩展函数 (处理了合并 1-3周 的逻辑)
        val weekDescription = course.weekRule.toDisplayText()

        val locale = Locale.getDefault()
        val periodDescription = stringResource(
            R.string.custom_periods_format,
            course.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
            course.startPeriod,
            course.startPeriod + course.duration - 1
        )

        // 2. 逻辑判断保持原样（使用你设定的 18 字符阈值）
        val isWeekTooLong = weekDescription.length > 18
        val isPeriodTooLong = periodDescription.length > 18

        if (isWeekTooLong || isPeriodTooLong) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailItem(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.week_number),
                    value = weekDescription
                )
                DetailItem(
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.period_number),
                    value = periodDescription
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailItem(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.week_number),
                    value = weekDescription
                )
                DetailItem(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.period_number),
                    value = periodDescription
                )
            }
        }
    }
}
/**
 * 💡 微调后的 DetailItem，增加了行高支持
 */
@Composable
fun DetailItem(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 22.sp, // 增加行高，多行时不拥挤
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
private fun IconInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp),
                tint = color
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionButtons(
    horizontally: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    if (horizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp), onClick = onEdit
            ) {
                Text(stringResource(R.string.edit_course), fontWeight = FontWeight.Bold)
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp), onClick = onEdit
            ) {
                Text(stringResource(R.string.edit_course), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.padding(6.dp))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}