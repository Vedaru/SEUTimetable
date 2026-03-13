package top.sakimidare.seutimetable.data.local

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import top.sakimidare.seutimetable.data.model.SemesterConfig
import top.sakimidare.seutimetable.data.model.WeekRule
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class TimetableConverters {

    // 💡 必须同时配置 LocalDate 和 LocalTime 的适配器
    private val gson: Gson = GsonBuilder()
        // LocalDate 适配器
        .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE))
        })
        .registerTypeAdapter(LocalDate::class.java, JsonDeserializer { json, _, _ ->
            LocalDate.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE)
        })
        // LocalTime 适配器
        .registerTypeAdapter(LocalTime::class.java, JsonSerializer<LocalTime> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_TIME))
        })
        .registerTypeAdapter(LocalTime::class.java, JsonDeserializer { json, _, _ ->
            LocalTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_TIME)
        })
        .create()

    @TypeConverter
    fun fromDayOfWeek(day: java.time.DayOfWeek?): Int? = day?.value

    @TypeConverter
    fun toDayOfWeek(value: Int?): java.time.DayOfWeek? = value?.let {
        java.time.DayOfWeek.of(it)
    }
    /* ---------- SemesterConfig 转换 ---------- */
    @TypeConverter
    fun fromSemesterConfig(config: SemesterConfig?): String? = config?.let { gson.toJson(it) }

    @TypeConverter
    fun toSemesterConfig(json: String?): SemesterConfig? = json?.let {
        gson.fromJson(it, SemesterConfig::class.java)
    }

    /* ---------- 基础时间类型支持 ---------- */
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    /* ---------- 颜色转换 ---------- */
    @TypeConverter
    fun fromColor(color: Color): Long = color.toArgb().toLong()

    @TypeConverter
    fun toColor(colorLong: Long): Color = Color(colorLong)

    /* ---------- 周规则转换 ---------- */
    @TypeConverter
    fun fromWeekRule(rule: WeekRule): String {
        return when (rule) {
            is WeekRule.All -> "ALL"
            is WeekRule.Odd -> "ODD"
            is WeekRule.Even -> "EVEN"
            is WeekRule.Custom -> "CUSTOM:${rule.weeks.joinToString(",")}"
        }
    }

    @TypeConverter
    fun toWeekRule(value: String): WeekRule {
        return when {
            value == "ALL" -> WeekRule.All
            value == "ODD" -> WeekRule.Odd
            value == "EVEN" -> WeekRule.Even
            value.startsWith("CUSTOM:") -> {
                val weeks = value.substringAfter("CUSTOM:").split(",")
                    .filter { it.isNotEmpty() }.map { it.toInt() }.toSet()
                WeekRule.Custom(weeks)
            }
            else -> WeekRule.All
        }
    }

}
