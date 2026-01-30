package top.sakimidare.seutimetable.ui.timetable.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import top.sakimidare.seutimetable.R
import top.sakimidare.seutimetable.data.model.WeekRule

@Composable
fun WeekRule.toDisplayText(): String {
    return when (this) {
        WeekRule.All -> stringResource(R.string.every_week)
        WeekRule.Odd -> stringResource(R.string.odd_week)
        WeekRule.Even -> stringResource(R.string.even_week)
        is WeekRule.Custom -> {
            val rangeText = formatRangeText(this.weeks)
            stringResource(R.string.custom_weeks_format, rangeText)
        }
    }
}

/**
 * 💡 核心算法：合并连续周
 * 例如 [1, 2, 3, 5, 8, 9] -> "1-3, 5, 8-9"
 */
private fun formatRangeText(weeks: Set<Int>): String {
    if (weeks.isEmpty()) return ""
    val sorted = weeks.sorted()
    val segments = mutableListOf<String>()

    var i = 0
    while (i < sorted.size) {
        val start = sorted[i]
        var end = start
        while (i + 1 < sorted.size && sorted[i + 1] == end + 1) {
            end = sorted[++i]
        }
        segments.add(if (start == end) "$start" else "$start-$end")
        i++
    }
    return segments.joinToString(", ")
}