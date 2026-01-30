package top.sakimidare.seutimetable.data.model

sealed class WeekRule {
    object All : WeekRule()                 // 每周
    object Odd : WeekRule()                 // 单周
    object Even : WeekRule()                // 双周
    data class Custom(val weeks: Set<Int>) : WeekRule() // 不规则

    companion object {
        fun fromWeeks(weeks: Set<Int>, totalWeeks: Int): WeekRule {
            return when {
                weeks.isEmpty() -> Custom(emptySet())
                weeks.size >= totalWeeks -> All
                weeks == (1..totalWeeks step 2).toSet() -> Odd
                weeks == (2..totalWeeks step 2).toSet() -> Even
                else -> Custom(weeks)
            }
        }
    }
}

/**
 * 当前周是否上课
 */
fun WeekRule.matches(week: Int): Boolean =
    when (this) {
        WeekRule.All -> true
        WeekRule.Odd -> week % 2 != 0
        WeekRule.Even -> week % 2 == 0
        is WeekRule.Custom -> week in weeks
    }

/**
 * 获取该规则对应的实际周数集合
 */
fun WeekRule.getActualWeeks(totalWeeks: Int): Set<Int> {
    return when (this) {
        WeekRule.All -> (1..totalWeeks).toSet()
        WeekRule.Odd -> (1..totalWeeks step 2).toSet()
        WeekRule.Even -> (2..totalWeeks step 2).toSet()
        is WeekRule.Custom -> this.weeks
    }
}