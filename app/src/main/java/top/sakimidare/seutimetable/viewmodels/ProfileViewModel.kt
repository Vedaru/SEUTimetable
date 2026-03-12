package top.sakimidare.seutimetable.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import top.sakimidare.seutimetable.R
import top.sakimidare.seutimetable.data.model.ProfileItem
import top.sakimidare.seutimetable.data.model.ProfileSection
import top.sakimidare.seutimetable.data.model.TableMetadata
import top.sakimidare.seutimetable.data.repository.UserPreferenceRepository

class ProfileViewModel(
    private val timetableViewModel: TimetableViewModel,
    private val prefRepository: UserPreferenceRepository
) : ViewModel() {

    // 1. 核心：将 DataStore 中的语言流与 Timetable 数据组合
    // 使用 stateIn 转化为 UI 状态流
    val uiState: StateFlow<ProfileItem.ProfileUiState> = combine(
        timetableViewModel.allTables,
        timetableViewModel.currentTable,
        timetableViewModel.studyStatistics,
        prefRepository.themeModeFlow,
        prefRepository.showTimelineFlow,
        prefRepository.showDateFlow,
        prefRepository.showPeriodTimeFlow,
        prefRepository.showNonCurrentWeekFlow,
        prefRepository.newsNotificationFlow
    ) { flows: Array<Any?> ->

        val allTables = flows[0] as List<*>
        val currentTable = flows[1] as TableMetadata?
        val stats = flows[2] as Pair<*, *>
        val themeMode = flows[3] as UserPreferenceRepository.ThemeMode
        val showTimeline = flows[4] as Boolean
        val showDate = flows[5] as Boolean
        val showPeriodTime = flows[6] as Boolean
        val showNonCurrentWeek = flows[7] as Boolean
        val notifyEnabled = flows[8] as Boolean

        ProfileItem.ProfileUiState(
            currentTableName = currentTable?.tableName ?: "",
            currentThemeMode = themeMode,
            completedLessons = stats.first as Int,
            totalHours = stats.second as Float,
            sections = listOf(
                ProfileSection(
                    titleRes = R.string.general_settings,
                    items = listOf(
                        ProfileItem.Action(Icons.Default.Palette, R.string.theme_setting) {
                            sendEvent(ProfileEvent.ShowThemeDialog)
                        },
                        ProfileItem.Action(Icons.Default.Language, R.string.language_setting) {
                            // show an in‑app dialog instead of launching a separate activity
                            sendEvent(ProfileEvent.ShowLanguageDialog)
                        }
                    )
                ),
                ProfileSection(
                    titleRes = R.string.display_settings, // 需在 strings.xml 定义
                    items = listOf(
                        ProfileItem.Toggle(
                            icon = Icons.Default.ViewWeek,
                            labelRes = R.string.show_timeline,
                            isChecked = showTimeline
                        ) { checked -> updatePreference { prefRepository.updateShowTimeline(checked) } },

                        ProfileItem.Toggle(
                            icon = Icons.Default.CalendarToday,
                            labelRes = R.string.show_date,
                            isChecked = showDate
                        ) { checked -> updatePreference { prefRepository.updateShowDate(checked) } },

                        ProfileItem.Toggle(
                            icon = Icons.Default.AccessTime,
                            labelRes = R.string.show_period_time,
                            isChecked = showPeriodTime
                        ) { checked -> updatePreference { prefRepository.updateShowPeriodTime(checked) } },

                        ProfileItem.Toggle(
                            icon = Icons.Default.FilterAlt,
                            labelRes = R.string.show_non_current_week,
                            isChecked = showNonCurrentWeek
                        ) { checked ->
                            updatePreference { prefRepository.updateShowNonCurrentWeek(checked) }
                        }
                    )
                ),
                // notification settings section
                ProfileSection(
                    titleRes = R.string.notification_settings,
                    items = listOf(
                        ProfileItem.Toggle(
                            icon = Icons.Default.Notifications,
                            labelRes = R.string.news_notifications,
                            isChecked = notifyEnabled
                        ) { checked -> updatePreference { prefRepository.updateNewsNotifications(checked) } }
                    )
                ),
                ProfileSection(
                    items = listOf(
                        ProfileItem.Action(Icons.Default.Info, R.string.about_app) {
                            sendEvent(ProfileEvent.ShowAbout)
                        }
                    )
                )
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileItem.ProfileUiState()
    )

    // 2. 处理主题切换逻辑：写入持久化存储
    fun updateThemeMode(mode: UserPreferenceRepository.ThemeMode) {
        viewModelScope.launch { prefRepository.updateThemeMode(mode) }
    }

    // language switching is now handled via the Profile screen dialog.

    // 3. 事件总线
    private val _events = MutableSharedFlow<ProfileEvent>()
    val events = _events.asSharedFlow()

    private fun sendEvent(event: ProfileEvent) {
        viewModelScope.launch { _events.emit(event) }
    }

    // 4. 更新 Factory 以支持两个参数
    companion object {
        fun provideFactory(
            timetableViewModel: TimetableViewModel,
            prefRepository: UserPreferenceRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfileViewModel(timetableViewModel, prefRepository) as T
                }
            }
    }

    private fun updatePreference(action: suspend () -> Unit) {
        viewModelScope.launch {
            action()
        }
    }
}

sealed class ProfileEvent {
        object ShowThemeDialog : ProfileEvent()
        // dialog used for picking a language inside the profile screen
        object ShowLanguageDialog : ProfileEvent()
        object NavigateToTableManager : ProfileEvent()
        object SyncData : ProfileEvent()
        object ShowAbout : ProfileEvent()
    }
