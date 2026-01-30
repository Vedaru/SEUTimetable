package top.sakimidare.seutimetable.ui.timetable.view.components
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun TimetableHeader(
    currentWeek: Int,
    startDate: java.time.LocalDate,
    sortedVisibleDays: List<java.time.DayOfWeek>,
    timeLabelWidth: androidx.compose.ui.unit.Dp,
    titleHeight: androidx.compose.ui.unit.Dp,
    locale: java.util.Locale = java.util.Locale.getDefault(),
    modifier: Modifier = Modifier
) {
    // 💡 内部逻辑：根据当前周和学期起点计算 7 天日期
    val dates = remember(currentWeek, startDate) {
        val startMonday = startDate.with(java.time.DayOfWeek.MONDAY)
        val targetMonday = startMonday.plusWeeks((currentWeek - 1).toLong())
        (0..6).map { targetMonday.plusDays(it.toLong()) }
    }

    Row(modifier = modifier.fillMaxWidth()) {
        // 左侧节次栏占位
        Spacer(modifier = Modifier.width(timeLabelWidth))

        sortedVisibleDays.forEach { day ->
            // DayOfWeek.MONDAY.value 是 1，所以对应索引是 day.value - 1
            val dateOfThisDay = dates[day.value - 1]
            val isToday = dateOfThisDay == java.time.LocalDate.now()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(titleHeight),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 星期几 (如：周一)
                Text(
                    text = day.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 具体日期 (如：01/28)
                Text(
                    text = String.format("%02d/%02d", dateOfThisDay.monthValue, dateOfThisDay.dayOfMonth),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}