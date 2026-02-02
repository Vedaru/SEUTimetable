package top.sakimidare.seutimetable.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.Period
import top.sakimidare.seutimetable.data.model.SemesterConfig
import top.sakimidare.seutimetable.data.model.TableMetadata
import top.sakimidare.seutimetable.data.repository.CourseRepository
import top.sakimidare.seutimetable.data.repository.UserPreferenceRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

private const val TAG = "TimetableViewModel"

class TimetableViewModel(
    private val courseRepository: CourseRepository,
    private val prefRepository: UserPreferenceRepository
) : ViewModel() {
    // --- 免责声明 ---
    val isDisclaimerAccepted = prefRepository.isDisclaimerAccepted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    // 初始值设为 true 以防闪烁，实际逻辑由 Flow 决定

    fun markDisclaimerAccepted() {
        viewModelScope.launch {
            prefRepository.updateDisclaimerAccepted(true)
        }
    }

    // --- 状态控制标志 ---
    private var isNavigatingToNewTable = false
    private var _isInternalWeekUpdate = false
    val isInternalWeekUpdate: Boolean get() = _isInternalWeekUpdate

    fun consumeInternalUpdate() {
        _isInternalWeekUpdate = false
    }

    /* -------------------------------------------------------
       1. 核心数据源 (Data Sources)
    ------------------------------------------------------- */

    /** 当前活跃课表 */
    val currentTable = courseRepository.observeCurrentTable()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentTableId = currentTable.map { it?.id }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val semesterConfig = currentTable.map {
        it?.semesterConfig ?: SemesterConfig.default()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SemesterConfig.default())

    val allTables = courseRepository.getAllTables()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allCourses = courseRepository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentTableCourses = combine(allCourses, currentTableId) { courses, tableId ->
        if (tableId == null) emptyList() else courses.filter { it.tableId == tableId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    /* -------------------------------------------------------
       2. 动态时间驱动 (Time Driven States)
    ------------------------------------------------------- */

    /** 每分钟更新一次的时间流，驱动 UI 实时感知课程状态 */
    val currentTime = flow {
        while (true) {
            emit(LocalTime.now())
            delay(30_000) // 30秒精度
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalTime.now())

    /** 当前正在进行的节次索引 (0-N)，不在上课时间则为 -1 */
    val activePeriodIndex = combine(semesterConfig, currentTime) { config, now ->
        config.periods.indexOfFirst { period ->
            !now.isBefore(period.start) && !now.isAfter(period.end)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    /* -------------------------------------------------------
       3. 过滤后的 UI 状态 (Filtered UI States)
    ------------------------------------------------------- */

    private val _currentWeek = MutableStateFlow(1)
    /** 用户正在看的周次 */
    val currentWeek = _currentWeek.asStateFlow()

    /** * 真正的“今日”周次（根据系统时间计算）
     * 即使 Pager 翻到了其他周，这个值也保持不变。
     */
    val actualCurrentWeek = semesterConfig.map { config ->
        val today = LocalDate.now()
        // 找到开学那周的周一
        val startMonday = config.startDate.with(java.time.DayOfWeek.MONDAY)
        val daysBetween = ChronoUnit.DAYS.between(startMonday, today)
        val week = (daysBetween / 7).toInt() + 1

        when {
            // 1. 开学日期之前，返回 null
            today.isBefore(config.startDate) -> null
            // 2. 超过总周数，返回 null
            week > config.weeks -> null
            // 3. 在 1..weeks 之间，返回真实周次
            else -> week
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 今天星期几 */
    val todayDayOfWeek = currentTime.map {
        LocalDate.now().dayOfWeek
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDate.now().dayOfWeek)

    /** 当前选定周的日期列表 */
    val currentWeekDates = combine(_currentWeek, semesterConfig) { week, config ->
        val startMonday = config.startDate.with(java.time.DayOfWeek.MONDAY)
        val targetMonday = startMonday.plusWeeks((week - 1).toLong())
        (0..6).map { targetMonday.plusDays(it.toLong()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 当前周可见的课程 (用于主课表视图) */
    val visibleCourses = combine(currentTableCourses, _currentWeek) { courses, week ->
        courses.filter { it.weekRule.matches(week) }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 今日需上的课程 (用于今日页面) */
    val todayCourses = combine(currentTableCourses, actualCurrentWeek, currentTime) { courses, week, _ ->
        // 💡 如果 week 为 null，说明不在学期内，直接返回空列表
        if (week == null) {
            emptyList()
        } else {
            val today = LocalDate.now().dayOfWeek
            courses.filter { it.dayOfWeek == today && it.weekRule.matches(week) }
                .sortedWith(compareBy({ it.startPeriod }, { it.duration }))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 当前正在上的课 */
    val activeCourse = combine(todayCourses, currentTime, semesterConfig) { courses, now, config ->
        courses.find { course ->
            val firstPeriod = config.periods.getOrNull(course.startPeriod - 1)
            val lastPeriod = config.periods.getOrNull(course.startPeriod + course.duration - 2)

            if (firstPeriod != null && lastPeriod != null) {
                // 💡 关键：只要时间在整门课的“大开头”和“大结尾”之间，都算 Active
                !now.isBefore(firstPeriod.start) && !now.isAfter(lastPeriod.end)
            } else false
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 下课休息 */
    val isInBreakTime = combine(activeCourse, activePeriodIndex, currentTime) { course, periodIndex, now ->
        // 如果有活跃课程，但当前时间不在任何具体的“节次”索引里，说明在课间
        course != null && periodIndex == -1
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** 下一节要上的课 */
    val nextCourse = combine(todayCourses, currentTime, semesterConfig, activeCourse) { courses, now, config, active ->
        courses.filter {
            val startTime = config.periods.getOrNull(it.startPeriod - 1)?.start
            it != active && startTime != null && startTime.isAfter(now)
        }.minByOrNull { it.startPeriod }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 当前课表已使用的最大节次 */
    val currentMaxPeriodInUse = currentTableCourses.map { courses ->
        courses.maxOfOrNull { it.startPeriod + it.duration - 1 } ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /* -------------------------------------------------------
       4. 逻辑初始化与周次校准
    ------------------------------------------------------- */

    init {
        viewModelScope.launch {
            currentTable.collectLatest { table ->
                if (isNavigatingToNewTable) {
                    isNavigatingToNewTable = false
                    return@collectLatest
                }
                table?.semesterConfig?.let { syncToActualWeek(it) }
            }
        }
    }

    private fun syncToActualWeek(config: SemesterConfig) {
        val today = LocalDate.now()
        val startMonday = config.startDate.with(java.time.DayOfWeek.MONDAY)
        val daysBetween = ChronoUnit.DAYS.between(startMonday, today)
        val calculatedWeek = (daysBetween / 7).toInt() + 1

        _currentWeek.value = when {
            today.isBefore(config.startDate) -> 1
            else -> calculatedWeek.coerceIn(1, config.weeks)
        }
    }

    /* -------------------------------------------------------
       5. 业务操作方法 (Operations)
    ------------------------------------------------------- */

    /* -------------------------------------------------------
       UI 交互：周次切换
    ------------------------------------------------------- */

    /** 切换到上一周 */
    fun prevWeek() {
        setWeek(_currentWeek.value - 1, fromPager = false)
    }

    /** 切换到下一周 */
    fun nextWeek() {
        setWeek(_currentWeek.value + 1, fromPager = false)
    }

    /** * 核心设置周次方法
     * @param fromPager 如果是 true，表示来自 ViewPager2/HorizontalPager 的滑动，不触发 scrollToPage 逻辑
     */
    fun setWeek(week: Int, fromPager: Boolean = false) {
        val maxWeeks = semesterConfig.value.weeks
        val targetWeek = week.coerceIn(1, maxWeeks)

        // 只有非 Pager 触发的更新（如点击按钮、自动同步）才标记为内部更新
        _isInternalWeekUpdate = !fromPager

        if (_currentWeek.value != targetWeek) {
            _currentWeek.value = targetWeek
        }
    }

    fun saveCourse(course: Course) {
        viewModelScope.launch {
            val toSave = if (course.id == 0L) {
                course.copy(tableId = currentTableId.value ?: return@launch)
            } else course
            courseRepository.saveCourse(toSave)
        }
    }

    fun removeCourse(courseId: Long) = viewModelScope.launch {
        courseRepository.deleteCourseById(courseId)
    }

    fun switchTable(targetTable: TableMetadata) {
        Log.d("WidgetFlow", "VM: Clicked Table ID = ${targetTable.id}")
        viewModelScope.launch {
            isNavigatingToNewTable = true
            courseRepository.switchTable(targetTable.id)
            syncToActualWeek(targetTable.semesterConfig)
        }
    }

    fun createTable(metadata: TableMetadata) = viewModelScope.launch {
        isNavigatingToNewTable = true
        _currentWeek.value = 1
        courseRepository.createNewTable(metadata.copy(id = 0L, isCurrent = true))
    }

    fun deleteTable(tableId: Long) = viewModelScope.launch {
        val currentList = allTables.value
        val isDeletingCurrent = (currentTableId.value == tableId)
        courseRepository.deleteTableById(tableId)
        if (isDeletingCurrent) {
            val remaining = currentList.filter { it.id != tableId }

            if (remaining.isNotEmpty()) {
                val nextTable = remaining.first()
                isNavigatingToNewTable = true
                courseRepository.switchTable(nextTable.id)
                syncToActualWeek(nextTable.semesterConfig)
            } else {
                _currentWeek.value = 1
            }
        }
    }

    fun updateTable(metadata: TableMetadata) = viewModelScope.launch {
        courseRepository.updateTableMetadata(metadata)
        if (_currentWeek.value > metadata.semesterConfig.weeks) {
            _currentWeek.value = metadata.semesterConfig.weeks
        }
    }

    fun getCourseById(id: Long) = allCourses.value.find { it.id == id }

    fun isPeriodPassed(period: Period): Boolean = LocalTime.now().isAfter(period.end)

    /* -------------------------------------------------------
       6. 课表导入工作流 (Import Flow)
    ------------------------------------------------------- */

    data class PendingImport(val metadata: TableMetadata, val courses: List<Course>)

    var isImporting by mutableStateOf(false); private set
    var pendingImport by mutableStateOf<PendingImport?>(null); private set
    var importErrorMessage by mutableStateOf<String?>(null); private set

    fun handleImportedJson(jsonString: String) = viewModelScope.launch {
        try {
            importErrorMessage = null
            isImporting = true
            isNavigatingToNewTable = true
            val (metadata, courses) = courseRepository.parseExternalJson(jsonString)
            pendingImport = PendingImport(metadata, courses)
        } catch (e: Exception) {
            isNavigatingToNewTable = false
            Log.e(TAG, "Failed to parse imported json", e)
            importErrorMessage = e.message ?: "解析失败"
        } finally {
            isImporting = false
        }
    }

    fun confirmImport(finalMetadata: TableMetadata) {
        val importData = pendingImport ?: return
        viewModelScope.launch {
            try {
                courseRepository.saveFullTable(finalMetadata, importData.courses)
                _currentWeek.value = 1
                pendingImport = null
                isNavigatingToNewTable = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save table", e)
                importErrorMessage = "保存失败"
            }
        }
    }

    fun cancelImport() {
        pendingImport = null; isNavigatingToNewTable = false
    }

    fun clearImportError() {
        importErrorMessage = null
    }
}

// --- Factory ---

class TimetableViewModelFactory(
    private val courseRepository: CourseRepository,
    private val prefRepository: UserPreferenceRepository // 💡 必须添加
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimetableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // 💡 传入两个 Repository
            return TimetableViewModel(courseRepository, prefRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}