package top.sakimidare.seutimetable.data.model

sealed class WeekRule {
    object All : WeekRule()                 // 每周
    object Odd : WeekRule()                 // 单周
    object Even : WeekRule()                // 双周
    data class Custom(val weeks: Set<Int>) : WeekRule() // 不规则

    /**
     * 判断两个规则是否在时间上有交集（即是否存在某一周，两者都要上课）
     */
    fun overlapsWith(other: WeekRule, totalWeeks: Int): Boolean {
        // 性能优化：快速判断一些显而易见的场景
        if (this is All || other is All) return true
        if (this is Odd && other is Even) return false
        if (this is Even && other is Odd) return false

        // 通用逻辑：判断集合是否有交集
        val myWeeks = this.getActualWeeks(totalWeeks)
        val otherWeeks = other.getActualWeeks(totalWeeks)
        return (myWeeks intersect otherWeeks).isNotEmpty()
    }

    /**
     * 当前周是否上课
     */
    fun matches(week: Int): Boolean =
        when (this) {
            All -> true
            Odd -> week % 2 != 0
            Even -> week % 2 == 0
            is Custom -> week in weeks
        }

    /**
     * 获取该规则对应的实际周数集合
     */
    fun getActualWeeks(totalWeeks: Int): Set<Int> {
        return when (this) {
            All -> (1..totalWeeks).toSet()
            Odd -> (1..totalWeeks step 2).toSet()
            Even -> (2..totalWeeks step 2).toSet()
            is Custom -> this.weeks
        }
    }
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