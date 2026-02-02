package top.sakimidare.seutimetable.ui.timetable.view.state

import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.TableMetadata

data class TimetableUiState(
    val showActionSheet: Boolean = false,
    val showDetailSheet: Boolean = false,
    val showEditDialog: Boolean = false,
    val showTableConfigDialog: Boolean = false,
    val showTableSelectSheet: Boolean = false,
    val showDeleteTableDialog: Boolean = false,
    val showDeleteCourseDialog: Boolean = false,
    val clickedCourse: Course? = null,
    val editingCourseId: Long = 0L,
    val tableToEdit: TableMetadata? = null
)