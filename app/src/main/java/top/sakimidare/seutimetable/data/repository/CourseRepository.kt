package top.sakimidare.seutimetable.data.repository

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import top.sakimidare.seutimetable.data.local.CourseDao
import top.sakimidare.seutimetable.data.local.TableDao
import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.Period
import top.sakimidare.seutimetable.data.model.SemesterConfig
import top.sakimidare.seutimetable.data.model.TableMetadata
import top.sakimidare.seutimetable.data.parser.TableParserUtils
import top.sakimidare.seutimetable.widgets.FORCE_REFRESH_KEY
import top.sakimidare.seutimetable.widgets.TodayWidget
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Date
import java.util.Locale

/**
 * 课表数据仓库
 * 协调 Room 数据库操作与 Widget 刷新逻辑，处理 JSON 数据解析
 */
class CourseRepository(
    private val courseDao: CourseDao,
    private val tableDao: TableDao,
    context: Context
) {
    private val appContext = context.applicationContext

    companion object {
        private const val TAG = "CourseRepository"
    }

    /* ---------- 1. 课程操作 (Course Actions) ---------- */

    val allCourses: Flow<List<Course>> = courseDao.getAllCourses()

    suspend fun saveCourse(course: Course) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Saving course: ${course.name}")
        courseDao.insertCourse(course)
        notifyWidgetUpdate()
    }

    suspend fun deleteCourseById(id: Long) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Deleting course ID: $id")
        courseDao.deleteById(id)
        notifyWidgetUpdate()
    }

    fun getCoursesByTableId(tableId: Long): Flow<List<Course>> =
        courseDao.getCoursesByTableId(tableId)

    /* ---------- 2. 课表元数据操作 (Table Actions) ---------- */

    fun getAllTables(): Flow<List<TableMetadata>> = tableDao.getAllTables()

    /**
     * 实时观察当前选中的课表配置
     */
    fun observeCurrentTable(): Flow<TableMetadata?> = tableDao.observeCurrentTable()

    suspend fun getTableById(id: Long): TableMetadata? = withContext(Dispatchers.IO) {
        tableDao.getTableById(id)
    }

    /**
     * 创建新课表并自动设为当前课表
     */
    suspend fun createNewTable(metadata: TableMetadata): Long = withContext(Dispatchers.IO) {
        val newId = tableDao.insertTable(metadata)
        Log.d(TAG, "New table created with ID: $newId")
        tableDao.setCurrentTable(newId)
        notifyWidgetUpdate()
        newId
    }

    /**
     * 切换活跃课表
     */
    suspend fun switchTable(tableId: Long) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Switching current table to: $tableId")
        tableDao.setCurrentTable(tableId)
        notifyWidgetUpdate()
    }

    suspend fun updateTableMetadata(metadata: TableMetadata) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Updating metadata for table: ${metadata.id}")
        tableDao.updateTable(metadata)
        notifyWidgetUpdate()
    }

    suspend fun deleteTableById(id: Long) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Deleting table and clearing cache for ID: $id")
        tableDao.deleteTableById(id)
        notifyWidgetUpdate()
    }

    /* ---------- 3. 导入逻辑 (Import Logic) ---------- */

    /**
     * 解析外部 JSON 字符串并推断课表配置
     */
    suspend fun parseExternalJson(jsonString: String): Pair<TableMetadata, List<Course>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting JSON parse process...")
        val root = JSONObject(jsonString)
        val termName = root.optString("termName", "导入课表")
        val rowsArray = root.optJSONArray("rows") ?: throw Exception("解析错误：未找到课程数据(rows)")

        if (rowsArray.length() == 0) throw Exception("解析错误：课表内容为空")

        val parseResult = TableParserUtils.parseCoursesFromJsonArray(rowsArray.toString(), 0L)
        Log.d(TAG, "Parse successful: Found ${parseResult.courses.size} courses")

        val defaultConfig = SemesterConfig.default()

        // 自动识别可见天数：若包含周末课程则显示全周
        val inferredVisibleDays = if (parseResult.hasWeekend) {
            DayOfWeek.entries.toSet()
        } else {
            DayOfWeek.entries.filter { it.value <= 5 }.toSet()
        }

        // 自动补全节次：若导入课程节次超过默认 13 节则自动扩展
        val finalPeriods = if (parseResult.maxPeriod > defaultConfig.periods.size) {
            defaultConfig.periods.toMutableList().apply {
                for (i in (size + 1)..parseResult.maxPeriod) {
                    add(Period(i, LocalTime.of(21, 30), LocalTime.of(22, 15)))
                }
            }
        } else defaultConfig.periods

        val timeStamp = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())

        val metadata = TableMetadata(
            tableName = "$termName ($timeStamp)",
            semesterConfig = defaultConfig.copy(
                weeks = parseResult.totalWeeks,
                visibleDays = inferredVisibleDays,
                periods = finalPeriods
            ),
            isCurrent = true
        )

        Pair(metadata, parseResult.courses)
    }

    /**
     * 保存完整的课表包：包含元数据和所有关联课程
     */
    suspend fun saveFullTable(metadata: TableMetadata, courses: List<Course>) = withContext(Dispatchers.IO) {
        Log.i(TAG, "Executing full table save: ${metadata.tableName}")

        // 1. 先保存元数据并获取 ID
        val newTableId = tableDao.insertTable(metadata)
        tableDao.setCurrentTable(newTableId)

        // 2. 映射课表 ID 到课程并批量插入
        val finalCourses = courses.map { it.copy(tableId = newTableId) }
        courseDao.insertCourses(finalCourses)

        Log.i(TAG, "Table saved successfully with ${finalCourses.size} courses")

        // 批量操作后统一刷新一次小组件
        notifyWidgetUpdate()
    }

    /* ---------- 4. 小组件同步 (Widget Synchronization) ---------- */

    /**
     * 触发 Glance 小组件状态更新
     */
    suspend fun notifyWidgetUpdate() {
        withContext(Dispatchers.IO) {
            try {
                val manager = GlanceAppWidgetManager(appContext)
                val glanceIds = manager.getGlanceIds(TodayWidget::class.java)

                if (glanceIds.isEmpty()) {
                    Log.d(TAG, "Widget update skipped: No active widgets found")
                    return@withContext
                }

                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(appContext, glanceId) { prefs ->
                        prefs[FORCE_REFRESH_KEY] = System.currentTimeMillis()
                    }
                    Log.d(TAG, "Widget state invalidated: $glanceId")
                }

                TodayWidget().updateAll(appContext)
                Log.i(TAG, "Widget update signal sent to ${glanceIds.size} instances")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update widget", e)
            }
        }
    }
}