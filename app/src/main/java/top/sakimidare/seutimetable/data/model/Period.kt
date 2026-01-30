package top.sakimidare.seutimetable.data.model

import java.time.LocalTime

data class Period(
    val index: Int,    // 第几节
    val start: LocalTime,
    val end: LocalTime
)