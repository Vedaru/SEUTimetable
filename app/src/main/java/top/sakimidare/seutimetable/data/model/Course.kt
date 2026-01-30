package top.sakimidare.seutimetable.data.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import top.sakimidare.seutimetable.data.local.AppDatabase
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
)