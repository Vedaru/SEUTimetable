package top.sakimidare.seutimetable.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.TableMetadata

@Database(
    entities = [Course::class, TableMetadata::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(TimetableConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun tableDao(): TableDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timetable_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}