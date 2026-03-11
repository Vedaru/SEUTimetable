package top.sakimidare.seutimetable.ui.timetable.utils


import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.sakimidare.seutimetable.data.model.Period
import java.time.Duration
import java.time.LocalTime

object TimetableGridUtils {
    /** 计算时间指示线的 Y 轴偏移量 */
    fun calculateTimeLineOffset(
        currentTime: LocalTime,
        periods: List<Period>,
        periodHeight: Dp
    ): Dp? {
        if (periods.isEmpty()) return null
        val first = periods.first()
        val last = periods.last()

        // debug: log early exits so we can see why the line vanished
        if (currentTime.isBefore(first.start) || currentTime.isAfter(last.end)) {
            android.util.Log.d("TimetableGridUtils", "timeLineOffset: out of bounds, current=$currentTime first=${first.start} last=${last.end}")
            return null
        }

        for (i in periods.indices) {
            val p = periods[i]
            if (!currentTime.isBefore(p.start) && !currentTime.isAfter(p.end)) {
                val total = Duration.between(p.start, p.end).toMinutes()
                val passed = Duration.between(p.start, currentTime).toMinutes()
                return (i + passed.toFloat() / total).dp * periodHeight.value
            } else if (i < periods.size - 1) {
                val nextP = periods[i + 1]
                if (currentTime.isAfter(p.end) && currentTime.isBefore(nextP.start)) {
                    android.util.Log.d("TimetableGridUtils", "timeLineOffset: in break between $p and $nextP at $currentTime")
                    return (i + 1).dp * periodHeight.value
                }
            }
        }
        android.util.Log.d("TimetableGridUtils", "timeLineOffset: no matching period found for $currentTime in list $periods")
        return null
    }

    /** 将不连续的节次索引（如 1, 2, 4, 5）拆分为块（如 [1,2], [4,5]） */
    fun findContinuousBlocks(periods: List<Int>): List<List<Int>> {
        if (periods.isEmpty()) return emptyList()
        val blocks = mutableListOf<MutableList<Int>>()
        var currentBlock = mutableListOf(periods[0])
        for (i in 1 until periods.size) {
            if (periods[i] == periods[i - 1] + 1) {
                currentBlock.add(periods[i])
            } else {
                blocks.add(currentBlock)
                currentBlock = mutableListOf(periods[i])
            }
        }
        blocks.add(currentBlock)
        return blocks
    }
}