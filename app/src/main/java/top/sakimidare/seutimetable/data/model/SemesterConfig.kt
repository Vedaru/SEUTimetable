package top.sakimidare.seutimetable.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class SemesterConfig(
    val startDate: LocalDate,
    val weeks: Int,
    val visibleDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val periods: List<Period>
) {
    /**
     * 根据当前配置，计算指定日期是第几周
     * @param targetDate 目标日期，默认为今天
     * @return 返回 1..weeks 之间的整数；如果不在学期范围内，则返回 null
     */
    fun calculateCurrentWeek(targetDate: LocalDate = LocalDate.now()): Int? {
        // 1. 校准到开学第一周的周一
        // 无论 startDate 是周几，课表的第一周通常从那个周的周一开始算
        val startMonday = startDate.with(DayOfWeek.MONDAY)

        // 2. 校准到学期结束周的周日 (即开学周一往后推 weeks 周)
        val endSunday = startMonday.plusWeeks(weeks.toLong()).minusDays(1)

        // 3. 边界检查
        if (targetDate.isBefore(startMonday) || targetDate.isAfter(endSunday)) {
            return null
        }

        // 4. 计算周次
        val daysBetween = ChronoUnit.DAYS.between(startMonday, targetDate)
        val calculatedWeek = (daysBetween / 7).toInt() + 1

        // 5. 限制区间并返回
        return calculatedWeek.coerceIn(1, weeks)
    }
    companion object {
        fun default(): SemesterConfig {
            val currentYear = LocalDate.now().year
            return SemesterConfig(
                startDate = LocalDate.of(currentYear, 3, 1),
                weeks = 16,
                visibleDays = DayOfWeek.entries.toSet(),
                periods = listOf(
                    // 上午
                    Period(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
                    Period(2, LocalTime.of(8, 50), LocalTime.of(9, 35)),
                    Period(3, LocalTime.of(9, 50), LocalTime.of(10, 35)),
                    Period(4, LocalTime.of(10, 40), LocalTime.of(11, 25)),
                    Period(5, LocalTime.of(11, 30), LocalTime.of(12, 15)),

                    // 下午
                    Period(6, LocalTime.of(14, 0), LocalTime.of(14, 45)),
                    Period(7, LocalTime.of(14, 50), LocalTime.of(15, 35)),
                    Period(8, LocalTime.of(15, 50), LocalTime.of(16, 35)),
                    Period(9, LocalTime.of(16, 40), LocalTime.of(17, 25)),
                    Period(10, LocalTime.of(17, 30), LocalTime.of(18, 15)),

                    // 晚上
                    Period(11, LocalTime.of(19, 0), LocalTime.of(19, 45)),
                    Period(12, LocalTime.of(19, 50), LocalTime.of(20, 35)),
                    Period(13, LocalTime.of(20, 40), LocalTime.of(21, 25))
                )
            )
        }
    }
}