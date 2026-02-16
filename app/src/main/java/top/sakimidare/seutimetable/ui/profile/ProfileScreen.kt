package top.sakimidare.seutimetable.ui.profile

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.sakimidare.seutimetable.R
import top.sakimidare.seutimetable.data.repository.UserPreferenceRepository
import top.sakimidare.seutimetable.viewmodels.ProfileEvent
import top.sakimidare.seutimetable.viewmodels.ProfileViewModel
import top.sakimidare.seutimetable.viewmodels.TimetableViewModel

@Composable
fun ProfileScreen(
    timetableViewModel: TimetableViewModel = viewModel(),
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass
) {
    val context = LocalContext.current
    val widthSizeClass = windowSizeClass.widthSizeClass
    val isExpanded =
        widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded

    // 1. 初始化 ViewModel，传入 Repository
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(
            timetableViewModel = timetableViewModel,
            prefRepository = UserPreferenceRepository(context.applicationContext)
        )
    )

    val state by profileViewModel.uiState.collectAsState()
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    LaunchedEffect(profileViewModel) {
        profileViewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowLanguageDialog -> showLanguagePicker = true
                ProfileEvent.NavigateToTableManager -> TODO()
                ProfileEvent.SyncData -> TODO()
                ProfileEvent.ShowAbout -> showAbout = true
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .let {
                    // 💡 如果是大屏（Expanded），限制最大宽度为 600dp-800dp
                    if (isExpanded) it.widthIn(max = 720.dp) else it.fillMaxSize()
                },
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "header") {
                ProfileHeader(
                    tableName = state.currentTableName.ifEmpty { stringResource(R.string.no_table_active) },
                    completedLessons = state.completedLessons,
                    totalHours = state.totalHours
                )
            }

            items(
                items = state.sections,
                key = { it.titleRes ?: it.hashCode() }
            ) { section ->
                ProfileSectionCard(
                    title = section.titleRes?.let { stringResource(it) },
                    items = section.items
                )
            }

            item(key = "footer_spacer") {
                Spacer(Modifier.height(88.dp))
            }
        }
    }

    if (showLanguagePicker) {
        LanguagePickerDialog(
            currentTag = state.currentLanguageTag,
            onSelected = { tag ->
                profileViewModel.updateLanguage(tag)
                (context as? Activity)?.let { activity ->
                    val intent = activity.intent
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)

                    activity.startActivity(intent)
                    activity.finish()
                }
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
fun LanguagePickerDialog(
    currentTag: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.language_setting),
                style = MaterialTheme.typography.titleLarge // 稍微调小一点，更精致
            )
        },
        confirmButton = {}, // 通常单选对话框不需要确定按钮，点选即关闭
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp) // 减少内边距
            ) {
                val languages = listOf(
                    stringResource(R.string.follow_system) to "", // 用空字符串代表跟随系统
                    "简体中文" to "zh-CN",
                    "English" to "en",
                    "日本語" to "ja"
                )

                languages.forEach { (name, tag) ->
                    val isSelected = if (tag.isEmpty()) {
                        currentTag.isEmpty() // 如果 tag 为空，检查当前 tag 是否也为空
                    } else {
                        currentTag.startsWith(tag.split("-")[0]) && currentTag.isNotEmpty()
                    }

                    LanguageItem(
                        name = name,
                        isSelected = isSelected,
                        onClick = { onSelected(tag) }
                    )
                }
            }
        }
    )
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.about_app),
                style = MaterialTheme.typography.titleLarge // 稍微调小一点，更精致
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp) // 减少内边距
            ) {
                Text(
                    text = stringResource(R.string.about_content),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    )
}

@Composable
private fun LanguageItem(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 使用图标或 RadioButton
        RadioButton(selected = isSelected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
