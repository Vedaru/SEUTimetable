package top.sakimidare.seutimetable.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "table_metadata")
data class TableMetadata(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableName: String,
    val semesterConfig: SemesterConfig,
    val isCurrent: Boolean = false
)