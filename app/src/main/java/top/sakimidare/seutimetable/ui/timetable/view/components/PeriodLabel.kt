package top.sakimidare.seutimetable.ui.timetable.view.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.sakimidare.seutimetable.data.model.Period
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Preview
@Composable
fun PeriodLabelPreview() {
    PeriodLabel(period = Period(1, LocalTime.of(8, 0), LocalTime.of(9, 30)))
}


@Composable
fun PeriodLabel(
    period: Period,
    isActive: Boolean = false // 💡 由 Grid 根据时间计算后传入
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // 根据激活状态动态计算样式
    val mainColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val subColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            // 💡 使用资源文件更专业，例如 R.string.period_num，值为 "%d"
            text = period.index.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = mainColor,
            fontWeight = fontWeight
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = period.start.format(timeFormatter),
            style = MaterialTheme.typography.labelSmall,
            color = subColor,
            fontWeight = fontWeight
        )
        Text(
            text = period.end.format(timeFormatter),
            style = MaterialTheme.typography.labelSmall,
            color = subColor,
            fontWeight = fontWeight
        )
    }
}