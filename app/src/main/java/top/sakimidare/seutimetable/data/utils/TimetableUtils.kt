package top.sakimidare.seutimetable.data.utils

import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.SemesterConfig
import top.sakimidare.seutimetable.data.model.WeekRule
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object TimetableUtils {
    /** 检查周规则冲突 */
    fun isWeekOverlap(r1: WeekRule, r2: WeekRule): Boolean {
        if (r1 is WeekRule.All || r2 is WeekRule.All) return true
        if ((r1 is WeekRule.Odd && r2 is WeekRule.Even) ||
            (r1 is WeekRule.Even && r2 is WeekRule.Odd)) return false

        return when {
            r1 is WeekRule.Custom -> checkCustomOverlap(r1, r2)
            r2 is WeekRule.Custom -> checkCustomOverlap(r2, r1)
            else -> r1.javaClass == r2.javaClass
        }
    }

    private fun checkCustomOverlap(custom: WeekRule.Custom, other: WeekRule): Boolean {
        return when (other) {
            WeekRule.All -> custom.weeks.isNotEmpty()
            WeekRule.Odd -> custom.weeks.any { it % 2 != 0 }
            WeekRule.Even -> custom.weeks.any { it % 2 == 0 }
            is WeekRule.Custom -> (custom.weeks intersect other.weeks).isNotEmpty()
        }
    }

    /** 检查单课冲突 */
    fun isCourseConflict(c1: Course, c2: Course): Boolean {
        if (c1.dayOfWeek != c2.dayOfWeek) return false
        val overlapPeriod = maxOf(c1.startPeriod, c2.startPeriod) <=
                minOf(c1.startPeriod + c1.duration - 1, c2.startPeriod + c2.duration - 1)
        return overlapPeriod && isWeekOverlap(c1.weekRule, c2.weekRule)
    }

    fun calculateCurrentWeek(config: SemesterConfig, targetDate: LocalDate = LocalDate.now()): Int? {
        // 1. 校准到开学第一周的周一
        val startMonday = config.startDate.with(java.time.DayOfWeek.MONDAY)

        // 2. 校准到学期结束周的周日 (即开学周一往后推 weeks 周)
        val endSunday = startMonday.plusWeeks(config.weeks.toLong()).minusDays(1)

        // 💡 边界检查：如果不在学期范围内，忠实返回 null
        if (targetDate.isBefore(startMonday) || targetDate.isAfter(endSunday)) {
            return null
        }

        // 3. 计算周次
        val daysBetween = ChronoUnit.DAYS.between(startMonday, targetDate)
        val calculatedWeek = (daysBetween / 7).toInt() + 1

        // 4. 虽然前面有边界检查，但为了保险依然做一次区间限制
        return calculatedWeek.coerceIn(1, config.weeks)
    }
}