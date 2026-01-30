package top.sakimidare.seutimetable.ui.timetable.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.sakimidare.seutimetable.R
import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.TableMetadata
import top.sakimidare.seutimetable.data.model.matches
import top.sakimidare.seutimetable.ui.timetable.edit.EmptyGuidePlaceholder
import top.sakimidare.seutimetable.ui.timetable.edit.TimetableAction
import top.sakimidare.seutimetable.ui.timetable.edit.TimetableActionSheet
import top.sakimidare.seutimetable.ui.timetable.edit.courses.DeleteCourseDialog
import top.sakimidare.seutimetable.ui.timetable.edit.courses.EditCourseDialog
import top.sakimidare.seutimetable.ui.timetable.edit.tables.DeleteTableDialog
import top.sakimidare.seutimetable.ui.timetable.edit.tables.TableConfigDialog
import top.sakimidare.seutimetable.ui.timetable.view.components.TimetableGrid
import top.sakimidare.seutimetable.ui.timetable.view.components.TimetableTopAppBar
import top.sakimidare.seutimetable.ui.timetable.view.sheets.CourseDetailSheet
import top.sakimidare.seutimetable.ui.timetable.view.sheets.TableSelectSheet
import top.sakimidare.seutimetable.viewmodels.TimetableViewModel
import top.sakimidare.seutimetable.widgets.requestPinTodayWidget


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    onImportRequest: () -> Unit,
    contentPadding: PaddingValues
) {
    // --- 1. 状态监听 ---
    val tableMetadata by viewModel.currentTable.collectAsState()
    val currentTableCourses by viewModel.currentTableCourses.collectAsState()
    val currentWeek by viewModel.currentWeek.collectAsState()
    val allTables by viewModel.allTables.collectAsState(initial = emptyList())
    val activePeriodIndex by viewModel.activePeriodIndex.collectAsState() // 💡 获取当前活跃节次

    // --- 2. 状态管理 ---
    var showActionSheet by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showTableConfigDialog by remember { mutableStateOf(false) }
    var showTableSelectSheet by remember { mutableStateOf(false) }
    var clickedCourse by remember { mutableStateOf<Course?>(null) }
    var editingCourseId by remember { mutableLongStateOf(0L) }
    var showDeleteTableDialog by remember { mutableStateOf(false) }
    var showDeleteCourseDialog by remember { mutableStateOf(false) }
    var tableToEdit by remember { mutableStateOf<TableMetadata?>(null) }

    val totalWeeks = tableMetadata?.semesterConfig?.weeks ?: 1
    // Pager 状态：与 ViewModel 的 currentWeek 双向绑定
    val pagerState = rememberPagerState(
        initialPage = (currentWeek - 1).coerceIn(0, (totalWeeks - 1).coerceAtLeast(0)),
        pageCount = { totalWeeks }
    )

    val context = LocalContext.current
// 1. 监听 Pager 的【目标页面】结算
// 关键：不再检查 isScrollInProgress，而是检查 pagerState.settledPage
    LaunchedEffect(pagerState.settledPage) {
        val targetWeek = pagerState.settledPage + 1
        // 只有当 Pager 停下来的页面和 ViewModel 不一致时，才通知 ViewModel
        if (currentWeek != targetWeek) {
            viewModel.setWeek(targetWeek, fromPager = true)
        }
    }

// 2. 监听 ViewModel 变化 -> 驱动 Pager 滚动
    LaunchedEffect(currentWeek) {
        val targetPage = (currentWeek - 1).coerceIn(0, (totalWeeks - 1).coerceAtLeast(0))
        if (pagerState.currentPage != targetPage) {
            if (viewModel.isInternalWeekUpdate) {
                // 切换课表：闪现
                pagerState.scrollToPage(targetPage)
                viewModel.consumeInternalUpdate()
            } else {
                // 标题栏点击：平滑滚动
                // 注意：这里执行时，上面的 settledPage 会在滚动完成后才触发，避免了即时冲突
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    val isImporting = viewModel.isImporting

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                tableMetadata?.let { metadata ->
                    TimetableTopAppBar(
                        timetableName = metadata.tableName,
                        currentWeek = currentWeek,
                        onTitleClick = { showTableSelectSheet = true },
                        canPrev = currentWeek > 1,
                        canNext = currentWeek < metadata.semesterConfig.weeks,
                        onPrev = { viewModel.prevWeek() },
                        onNext = { viewModel.nextWeek() }
                    )
                } ?: TopAppBar(title = { Text(stringResource(R.string.timetable)) })

                if (isImporting) {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isImporting) {
                FloatingActionButton(onClick = { showActionSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.operating_menu))
                }
            }
        },
        bottomBar = { Spacer(Modifier.height(contentPadding.calculateBottomPadding())) }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding).fillMaxSize()) {
            if (tableMetadata == null) {
                EmptyGuidePlaceholder(
                    onImport = onImportRequest,
                    onCreate = { showTableConfigDialog = true }
                )
            } else {
                // --- 💡 核心：带 Page 切换的课表网格 ---
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    val weekForThisPage = pageIndex + 1

                    // 过滤当前页显示的课程
                    val coursesForThisPage = remember(currentTableCourses, weekForThisPage) {
                        currentTableCourses.filter { it.weekRule.matches(weekForThisPage) }
                    }

                    TimetableGrid(
                        tableMetadata = tableMetadata!!,
                        courses = coursesForThisPage,
                        currentWeek = weekForThisPage, // 💡 必须传入，用于 Header 日期计算
                        activePeriodIndex = if (weekForThisPage == currentWeek) activePeriodIndex else -1,
                        onCourseClick = { course ->
                            clickedCourse = course
                            showDetailSheet = true
                        }
                    )
                }
            }

            // --- 5. 弹窗集散地 ---

            // A. FAB 操作菜单 (ModalBottomSheet 会自动覆盖 NavigationBar)
            if (showActionSheet) {
                TimetableActionSheet(
                    onActionClick = { action ->
                        showActionSheet = false
                        when (action) {
                            TimetableAction.Import -> onImportRequest()
                            TimetableAction.AddCourse -> {
                                if (tableMetadata == null) showTableConfigDialog = true
                                else {
                                    editingCourseId = 0L // 新建模式
                                    showEditDialog = true
                                }
                            }

                            TimetableAction.CreateEmpty -> showTableConfigDialog = true
                            TimetableAction.Switch -> {
                                showTableSelectSheet = true
                            }

                            TimetableAction.Delete -> {
                                showDeleteTableDialog = true
                            }

                            TimetableAction.EditConfig -> {
                                tableToEdit = tableMetadata // 将当前活跃课表存入待编辑状态
                                showTableConfigDialog = true // 唤起我们之前改造好的全能 Dialog
                            }
                            TimetableAction.AddShortCut -> {
                                requestPinTodayWidget(context = context)
                            }
                        }
                    },
                    onDismissRequest = { showActionSheet = false },
                    timetableCount = allTables.size
                )
            }
            // B. 课程详情抽屉 (你原有的逻辑)
            if (showDetailSheet && clickedCourse != null) {
                CourseDetailSheet(
                    onDismissRequest = { showDetailSheet = false },
                    course = clickedCourse!!,
                    onEdit = {
                        editingCourseId = clickedCourse!!.id // 进入编辑模式
                        showDetailSheet = false
                        showEditDialog = true
                    },
                    onDelete = {
                        showDeleteCourseDialog = true
                        showDetailSheet = false
                    },
                    sheetState = rememberModalBottomSheetState(),
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                )
            }
            // --- C. 新增/编辑对话框 ---
            if (showEditDialog) {
                tableMetadata?.let { metadata ->
                    EditCourseDialog(
                        tableMetadata = metadata,
                        // 💡 改用 currentTableCourses，确保只检测当前课表的冲突
                        existingCourses = currentTableCourses,
                        currentCourseId = editingCourseId,
                        onDismiss = { showEditDialog = false },
                        onSave = { updatedCourse ->
                            viewModel.saveCourse(updatedCourse)
                            showEditDialog = false
                        }
                    )
                }
            }

            // D. 创建课表对话框
            if (showTableConfigDialog) {
                TableConfigDialog(
                    initialMetadata = tableToEdit, // 💡 传入当前正在编辑的课表（新建则为 null）
                    onDismiss = {
                        showTableConfigDialog = false
                        tableToEdit = null // 关闭时重置
                    },
                    maxPeriodInUse = viewModel.currentMaxPeriodInUse.collectAsState().value,
                    onConfirm = { tableMetadata ->
                        if (tableToEdit == null) {
                            viewModel.createTable(tableMetadata)
                        } else {
                            viewModel.updateTable(tableMetadata)
                        }
                        showTableConfigDialog = false
                        tableToEdit = null
                    }
                )
            }

            if (showTableSelectSheet) {
                TableSelectSheet(
                    tables = allTables,
                    currentTableId = tableMetadata?.id ?: -1L,
                    onTableSelect = { table ->
                        viewModel.switchTable(table) // 调用 ViewModel 切换当前课表
                        showTableSelectSheet = false
                    },
                    onCreateNew = {
                        showTableConfigDialog = true // 触发你之前的创建课表弹窗
                    },
                    onImport = onImportRequest,
                    onDismissRequest = { showTableSelectSheet = false }
                )
            }

            // --- 弹窗集散地 ---

// A. 删除课表
            if (showDeleteTableDialog) {
                DeleteTableDialog(
                    tableName = tableMetadata?.tableName ?: "",
                    onDismiss = { showDeleteTableDialog = false },
                    onConfirm = {
                        tableMetadata?.let { viewModel.deleteTable(it.id) }
                        showDeleteTableDialog = false
                    }
                )
            }

// B. 删除课程
            if (showDeleteCourseDialog && clickedCourse != null) {
                DeleteCourseDialog(
                    courseName = clickedCourse!!.name,
                    onDismiss = { showDeleteCourseDialog = false },
                    onConfirm = {
                        viewModel.removeCourse(clickedCourse!!.id)
                        showDeleteCourseDialog = false
                        showDetailSheet = false
                    }
                )
            }
        }
    }
}