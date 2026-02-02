package top.sakimidare.seutimetable.ui.main.state

import androidx.compose.runtime.Composable
import top.sakimidare.seutimetable.ui.importing.ImportWebViewScreen
import top.sakimidare.seutimetable.viewmodels.TimetableViewModel

@Composable
fun ImportOverlay(
    viewModel: TimetableViewModel,
    showWebView: Boolean,
    onDismissWebView: () -> Unit
) {
    val pendingImport = viewModel.pendingImport

    // WebView 阶段
    if (showWebView) {
        ImportWebViewScreen(
            onDismiss = onDismissWebView,
            onDataAcquired = { json ->
                onDismissWebView()
                viewModel.handleImportedJson(json)
            }
        )
    }

    // 配置确认阶段
    pendingImport?.let { data ->
        top.sakimidare.seutimetable.ui.timetable.edit.tables.TableConfigDialog(
            initialMetadata = data.metadata,
            maxPeriodInUse = data.courses.maxOfOrNull { it.startPeriod + it.duration - 1 } ?: 0,
            onDismiss = { viewModel.cancelImport() },
            onConfirm = { finalMetadata -> viewModel.confirmImport(finalMetadata) }
        )
    }
}