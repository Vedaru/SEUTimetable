package top.sakimidare.seutimetable.data.model
import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = TableMetadata::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableId: Long,
    val name: String,
    val teacher: String,
    val location: String,
    val dayOfWeek: DayOfWeek,
    val startPeriod: Int,
    val duration: Int,
    val color: Color,
    val weekRule: WeekRule = WeekRule.All,
    val note: String,
) {

    /**
     * 判断当前课程是否与另一个课程存在“时空冲突”
     * * @param other 另一个课程对象
     * @param totalWeeks 用于周次规则解析的总周数
     * @return 如果在同一天、同一节次、且周次有交集，返回 true
     */
    fun isConflictingWith(other: Course, totalWeeks: Int = 25): Boolean {
        // 1. 基本校验：不同课表的课程不冲突；自己不跟自己冲突
        if (this.tableId != other.tableId) return false
        if (this.id != 0L && this.id == other.id) return false

        // 2. 空间检测：星期是否相同
        if (this.dayOfWeek != other.dayOfWeek) return false

        // 3. 时间检测：节次是否有交集 [start, start + duration - 1]
        val thisEnd = this.startPeriod + this.duration - 1
        val otherEnd = other.startPeriod + other.duration - 1
        val periodOverlap = maxOf(this.startPeriod, other.startPeriod) <= minOf(thisEnd, otherEnd)

        if (!periodOverlap) return false

        // 4. 精度检测：周次是否有交集 (利用 WeekRule 已有的重叠判断逻辑)
        return this.weekRule.overlapsWith(other.weekRule, totalWeeks)
    }

    fun getStartTime(config: SemesterConfig) = config.periods.getOrNull(this.startPeriod - 1)?.start
    fun getEndTime(config: SemesterConfig) = config.periods.getOrNull(this.startPeriod + this.duration - 2)?.end

}
