package top.sakimidare.seutimetable.ui.timetable.view.components

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.TableMetadata
import top.sakimidare.seutimetable.ui.timetable.edit.TimetableAction
import top.sakimidare.seutimetable.ui.timetable.edit.TimetableActionSheet
import top.sakimidare.seutimetable.ui.timetable.edit.courses.EditCourseDialog
import top.sakimidare.seutimetable.ui.timetable.edit.tables.DeleteTableDialog
import top.sakimidare.seutimetable.ui.timetable.edit.tables.TableConfigDialog
import top.sakimidare.seutimetable.ui.timetable.view.sheets.CourseDetailSheet
import top.sakimidare.seutimetable.ui.timetable.view.sheets.TableSelectSheet
import top.sakimidare.seutimetable.ui.timetable.view.state.TimetableUiState
import top.sakimidare.seutimetable.viewmodels.TimetableViewModel
import top.sakimidare.seutimetable.widgets.requestPinTodayWidget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableOverlayHost(
    state: TimetableUiState,
    onStateChange: (TimetableUiState) -> Unit,
    viewModel: TimetableViewModel,
    onImportRequest: () -> Unit,
    currentWeek: Int,
    currentTableCourses: List<Course>,
    allTables: List<TableMetadata>
) {
    val context = LocalContext.current

    // 💡 响应式观察 ViewModel 状态，禁止使用 .value 直接读取
    val currentTable by viewModel.currentTable.collectAsState()
    val maxPeriodInUse by viewModel.currentMaxPeriodInUse.collectAsState()

    // A. FAB 操作菜单
    if (state.showActionSheet) {
        TimetableActionSheet(
            onActionClick = { action ->
                val baseState = state.copy(showActionSheet = false)
                when (action) {
                    TimetableAction.Import -> {
                        onImportRequest()
                    }
                    TimetableAction.AddCourse -> {
                        if (currentTable == null) {
                            onStateChange(baseState.copy(showTableConfigDialog = true))
                        } else {
                            onStateChange(baseState.copy(editingCourseId = 0L, showEditDialog = true))
                        }
                    }
                    TimetableAction.CreateEmpty -> onStateChange(baseState.copy(showTableConfigDialog = true))
                    TimetableAction.Switch -> onStateChange(baseState.copy(showTableSelectSheet = true))
                    TimetableAction.Delete -> onStateChange(baseState.copy(showDeleteTableDialog = true))
                    TimetableAction.EditConfig -> {
                        onStateChange(baseState.copy(tableToEdit = currentTable, showTableConfigDialog = true))
                    }
                    TimetableAction.AddShortCut -> {
                        requestPinTodayWidget(context)
                        onStateChange(baseState)
                    }
                }
            },
            onDismissRequest = { onStateChange(state.copy(showActionSheet = false)) },
            timetableCount = allTables.size
        )
    }

    // B. 课程详情 (将状态移入 if 块，确保每次打开都是重置的)
    if (state.showDetailSheet && state.clickedCourse != null) {
        val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        CourseDetailSheet(
            course = state.clickedCourse,
            onDismissRequest = { onStateChange(state.copy(showDetailSheet = false)) },
            onEdit = {
                onStateChange(state.copy(
                    editingCourseId = state.clickedCourse.id,
                    showDetailSheet = false,
                    showEditDialog = true
                ))
            },
            onDelete = {
                onStateChange(state.copy(showDeleteCourseDialog = true, showDetailSheet = false))
            },
            currentWeek = currentWeek,
            sheetState = detailSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        )
    }

    // C. 编辑/新增课程对话框
    if (state.showEditDialog) {
        currentTable?.let { metadata ->
            EditCourseDialog(
                tableMetadata = metadata,
                existingCourses = currentTableCourses,
                currentCourseId = state.editingCourseId,
                onDismiss = { onStateChange(state.copy(showEditDialog = false)) },
                onSave = { updatedCourse ->
                    viewModel.saveCourse(updatedCourse)
                    onStateChange(state.copy(showEditDialog = false))
                }
            )
        }
    }

    // D. 课表配置对话框 (修改/创建课表)
    if (state.showTableConfigDialog) {
        TableConfigDialog(
            initialMetadata = state.tableToEdit,
            onDismiss = { onStateChange(state.copy(showTableConfigDialog = false, tableToEdit = null)) },
            maxPeriodInUse = maxPeriodInUse,
            onConfirm = { metadata ->
                if (state.tableToEdit == null) viewModel.createTable(metadata)
                else viewModel.updateTable(metadata)
                onStateChange(state.copy(showTableConfigDialog = false, tableToEdit = null))
            }
        )
    }

    // E. 课表切换选择器
    if (state.showTableSelectSheet) {
        TableSelectSheet(
            tables = allTables,
            currentTableId = currentTable?.id ?: -1L,
            onTableSelect = { table ->
                viewModel.switchTable(table)
                onStateChange(state.copy(showTableSelectSheet = false))
            },
            onCreateNew = { onStateChange(state.copy(showTableConfigDialog = true, showTableSelectSheet = false)) },
            onImport = onImportRequest,
            onDismissRequest = { onStateChange(state.copy(showTableSelectSheet = false)) }
        )
    }

    // F. 删除确认对话框 (课表)
    if (state.showDeleteTableDialog) {
        currentTable?.let { table ->
            DeleteTableDialog(
                tableName = table.tableName,
                onDismiss = { onStateChange(state.copy(showDeleteTableDialog = false)) },
                onConfirm = {
                    viewModel.deleteTable(table.id)
                    onStateChange(state.copy(showDeleteTableDialog = false))
                }
            )
        }
    }
}