package top.sakimidare.seutimetable.ui.profile

import android.widget.Toast
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
import top.sakimidare.seutimetable.data.model.ProfileItem
import top.sakimidare.seutimetable.data.repository.UserPreferenceRepository
import top.sakimidare.seutimetable.LocaleManager
import top.sakimidare.seutimetable.notifications.NewsUpdateWorker
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

    // attach localized trailing text for theme and language entries
    val sections = state.sections.map { section ->
        val items = section.items.map { item ->
            when {
                item is ProfileItem.Action && item.labelRes == R.string.theme_setting -> {
                    val themeLabel = when (state.currentThemeMode) {
                        UserPreferenceRepository.ThemeMode.LIGHT -> stringResource(R.string.light)
                        UserPreferenceRepository.ThemeMode.DARK -> stringResource(R.string.dark)
                        UserPreferenceRepository.ThemeMode.SYSTEM -> stringResource(R.string.follow_system)
                    }
                    item.copy(trailing = themeLabel)
                }
                item is ProfileItem.Action && item.labelRes == R.string.language_setting -> {
                    val lang = LocaleManager.getLanguage(context)
                    val label = if (lang == LocaleManager.Language.SYSTEM) {
                        stringResource(R.string.follow_system)
                    } else {
                        lang.displayName(context)
                    }
                    item.copy(trailing = label)
                }
                else -> item
            }
        }
        section.copy(items = items)
    }
    // state flags for modal dialogs
    var showThemePicker by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    LaunchedEffect(profileViewModel) {
        profileViewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowThemeDialog -> showThemePicker = true
                is ProfileEvent.ShowLanguageDialog -> showLanguagePicker = true
                is ProfileEvent.ShowAbout -> showAbout = true
                is ProfileEvent.NavigateToTableManager -> {
                    // TODO: Implement navigation to Table Manager
                }
                is ProfileEvent.SyncData -> {
                    // TODO: Implement sync data
                }
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
                items = sections,
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

    if (showThemePicker) {
        ThemePickerDialog(
            currentMode = state.currentThemeMode,
            onSelected = { mode ->
                profileViewModel.updateThemeMode(mode)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false }
        )
    }

    if (showLanguagePicker) {
        val currentLang = LocaleManager.getLanguage(context)
        LanguagePickerDialog(
            current = currentLang,
            onSelected = { lang ->
                LocaleManager.setLanguage(context, lang)
                var activity: android.app.Activity? = null
                var ctx: android.content.Context? = context
                while (ctx is android.content.ContextWrapper && activity == null) {
                    if (ctx is android.app.Activity) {
                        activity = ctx
                    } else {
                        ctx = ctx.baseContext
                    }
                }
                activity?.recreate()
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false }
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

// --- generic option dialog ------------------------------------------------
@Composable
fun <T> OptionPickerDialog(
    titleRes: Int,
    options: List<Pair<String, T>>,
    currentSelection: T,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge
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
                    .padding(vertical = 0.dp)
            ) {
                options.forEach { (name, value) ->
                    val isSelected = value == currentSelection
                    OptionItem(
                        name = name,
                        isSelected = isSelected,
                        onClick = {
                            android.util.Log.d("ProfileScreen", "option clicked: $value")
                            onSelected(value)
                        }
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
                style = MaterialTheme.typography.titleLarge
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
                    .padding(vertical = 0.dp)
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
fun ThemePickerDialog(
    currentMode: UserPreferenceRepository.ThemeMode,
    onSelected: (UserPreferenceRepository.ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        stringResource(R.string.follow_system) to UserPreferenceRepository.ThemeMode.SYSTEM,
        stringResource(R.string.light) to UserPreferenceRepository.ThemeMode.LIGHT,
        stringResource(R.string.dark) to UserPreferenceRepository.ThemeMode.DARK
    )
    OptionPickerDialog<UserPreferenceRepository.ThemeMode>(
        titleRes = R.string.theme_setting,
        options = options,
        currentSelection = currentMode,
        onSelected = onSelected,
        onDismiss = onDismiss
    )
}

@Composable
fun LanguagePickerDialog(
    current: LocaleManager.Language,
    onSelected: (LocaleManager.Language) -> Unit,
    onDismiss: () -> Unit
) {
    val options = LocaleManager.Language.values().map { lang ->
        lang.displayName(LocalContext.current) to lang
    }
    OptionPickerDialog<LocaleManager.Language>(
        titleRes = R.string.language_setting,
        options = options,
        currentSelection = current,
        onSelected = onSelected,
        onDismiss = onDismiss
    )
}

@Composable
private fun OptionItem(
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
