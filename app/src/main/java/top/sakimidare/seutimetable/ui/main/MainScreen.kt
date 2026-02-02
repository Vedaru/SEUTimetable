package top.sakimidare.seutimetable.ui.main

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import top.sakimidare.seutimetable.R
import top.sakimidare.seutimetable.ui.main.state.ImportOverlay
import top.sakimidare.seutimetable.ui.news.NewsScreen
import top.sakimidare.seutimetable.ui.profile.ProfileScreen
import top.sakimidare.seutimetable.ui.timetable.view.TimetableScreen
import top.sakimidare.seutimetable.ui.today.TodayScreen
import top.sakimidare.seutimetable.viewmodels.MainViewModel
import top.sakimidare.seutimetable.viewmodels.TimetableViewModel

@Composable
fun MainScreen(
    windowSizeClass: WindowSizeClass,
    timetableViewModel: TimetableViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val isAccepted by timetableViewModel.isDisclaimerAccepted.collectAsState()
    val context = LocalContext.current

    when (isAccepted) {
        null -> {}
        false -> DisclaimerDialog(
            onConfirm = { timetableViewModel.markDisclaimerAccepted() },
            onTerminate = { (context as? Activity)?.finish() }
        )

        true -> MainScaffoldContent(windowSizeClass, timetableViewModel, mainViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffoldContent(
    windowSizeClass: WindowSizeClass,
    timetableViewModel: TimetableViewModel,
    mainViewModel: MainViewModel
) {
    val currentTab by mainViewModel.currentTab.collectAsState()
    val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    var showImportWebView by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Row(Modifier.fillMaxSize()) {
        if (useNavRail) {
            MainNavigationRail(currentTab) { mainViewModel.updateTab(it) }
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            topBar = {
                when (currentTab) {
                    MainTab.News -> TopAppBar(title = { Text(stringResource(R.string.news)) })
                    MainTab.Profile -> TopAppBar(title = { Text(stringResource(R.string.me)) })
                    MainTab.Timetable -> { /* 内容由 TimetableScreen 内部控制，此处不放东西 */
                    }

                    MainTab.Today -> { /* 今日课程页无TopAppBar */
                    }
                }
            },
            bottomBar = {
                if (!useNavRail) MainNavigationBar(currentTab) { mainViewModel.updateTab(it) }
            }
        ) { innerPadding ->
            MainTabContent(
                currentTab,
                timetableViewModel,
                mainViewModel,
                innerPadding,
                windowSizeClass,
                onImportRequest = { showImportWebView = true })
        }
    }

    ImportOverlay(
        timetableViewModel,
        showImportWebView,
        onDismissWebView = { showImportWebView = false })

    // 错误处理
    timetableViewModel.importErrorMessage?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            timetableViewModel.clearImportError()
        }
    }
}

@Composable
fun MainTabContent(
    currentTab: MainTab,
    timetableViewModel: TimetableViewModel,
    mainViewModel: MainViewModel,
    contentPadding: PaddingValues,
    windowSizeClass: WindowSizeClass,
    onImportRequest: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        when (currentTab) {
            MainTab.Today -> TodayScreen(
                viewModel = timetableViewModel,
                contentPadding = contentPadding,
                windowSize = windowSizeClass.widthSizeClass,
                onNavigateToTimetable = { mainViewModel.updateTab(MainTab.Timetable) }
            )

            MainTab.Timetable -> TimetableScreen(
                viewModel = timetableViewModel,
                onImportRequest = onImportRequest,
                contentPadding = contentPadding
            )

            MainTab.News -> Box(Modifier.padding(contentPadding)) {
                NewsScreen()
            }

            MainTab.Profile -> Box(Modifier.padding(contentPadding)) {
                ProfileScreen()
            }
        }
    }
}