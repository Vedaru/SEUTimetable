package top.sakimidare.seutimetable.ui.main

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.sakimidare.seutimetable.R
import top.sakimidare.seutimetable.ui.importing.ImportWebViewScreen
import top.sakimidare.seutimetable.ui.news.NewsScreen
import top.sakimidare.seutimetable.ui.profile.ProfileScreen
import top.sakimidare.seutimetable.ui.timetable.view.TimetableScreen
import top.sakimidare.seutimetable.ui.today.TodayScreen
import top.sakimidare.seutimetable.viewmodels.TimetableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    windowSizeClass: WindowSizeClass,
    timetableViewModel: TimetableViewModel = viewModel()
) {
    val isAccepted by timetableViewModel.isDisclaimerAccepted.collectAsState()
    val context = LocalContext.current
    when (isAccepted){
        null -> { /* 空白页，避免瞬间闪烁 */ }

        false -> {
            DisclaimerDialog(
                onConfirm = { timetableViewModel.markDisclaimerAccepted() },
                onTerminate = { (context as? Activity)?.finish() }
            )
        }

        true -> {
            var currentTab by remember { mutableStateOf<MainTab>(MainTab.Timetable) }
            val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
            var showImportWebView by remember { mutableStateOf(false) }

            val pendingImport = timetableViewModel.pendingImport
            val importError = timetableViewModel.importErrorMessage

            Row(Modifier.fillMaxSize()) {
                if (useNavRail) {
                    NavigationRail {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
                        ) {
                            Spacer(Modifier.weight(1f))
                            MainTab.items.forEach { tab ->
                                NavigationRailItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(tab.icon, null) },
                                    label = { Text(stringResource(tab.titleRes)) }
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }

                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    topBar = {
                        when (currentTab) {
                            MainTab.News -> TopAppBar(title = { Text(stringResource(R.string.news)) })
                            MainTab.Profile -> TopAppBar(title = { Text(stringResource(R.string.me)) })
                            MainTab.Timetable -> { /* 内容由 TimetableScreen 内部控制，此处不放东西 */ }
                            MainTab.Today -> { /* 今日课程页无TopAppBar */ }
                        }
                    },
                    bottomBar = {
                        if (!useNavRail) {
                            NavigationBar {
                                MainTab.items.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = { currentTab = tab },
                                        icon = { Icon(tab.icon, null) },
                                        label = { Text(stringResource(tab.titleRes)) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(Modifier.fillMaxSize()) {
                        when (currentTab) {
                            MainTab.Today -> TodayScreen(
                                viewModel = timetableViewModel,
                                contentPadding = innerPadding,
                                windowSize = windowSizeClass.widthSizeClass,
                                onNavigateToTimetable = { currentTab = MainTab.Timetable }
                            )
                            MainTab.Timetable -> TimetableScreen(
                                viewModel = timetableViewModel,
                                onImportRequest = { showImportWebView = true },
                                contentPadding = innerPadding
                            )

                            MainTab.News -> Box(Modifier.padding(innerPadding)) { NewsScreen() }
                            MainTab.Profile -> Box(Modifier.padding(innerPadding)) { ProfileScreen() }
                        }
                    }
                }
            }

            // --- 1. WebView 抓取阶段 ---
            if (showImportWebView) {
                ImportWebViewScreen(
                    onDismiss = { showImportWebView = false },
                    onDataAcquired = { json ->
                        showImportWebView = false
                        // 💡 调用 handleImportedJson 进入“待确认”状态
                        timetableViewModel.handleImportedJson(json)
                    }
                )
            }

            // --- 2. 导入确认阶段 (TableConfigDialog) ---
            // 只要 pendingImport 不为空，说明刚抓完数据，立即弹出配置框
            if (pendingImport != null) {
                top.sakimidare.seutimetable.ui.timetable.edit.tables.TableConfigDialog(
                    initialMetadata = pendingImport.metadata,
                    maxPeriodInUse = pendingImport.courses.maxOfOrNull { it.startPeriod + it.duration - 1 } ?: 0,
                    onDismiss = { timetableViewModel.cancelImport() },
                    onConfirm = { finalMetadata ->
                        timetableViewModel.confirmImport(finalMetadata)
                    }
                )
            }
            if (importError != null) {
                LaunchedEffect(importError) {
                    Toast.makeText(context, importError, Toast.LENGTH_LONG).show()
                    timetableViewModel.clearImportError()
                }
            }
        }
    }
}