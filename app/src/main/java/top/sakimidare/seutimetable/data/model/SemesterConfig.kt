package top.sakimidare.seutimetable.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class SemesterConfig(
    val startDate: LocalDate,
    val weeks: Int,
    val visibleDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val periods: List<Period>
) {
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