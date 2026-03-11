package top.sakimidare.seutimetable.data.model

import androidx.compose.ui.graphics.vector.ImageVector

sealed class ProfileItem {
    data class ProfileUiState(
        val sections: List<ProfileSection> = emptyList(),
        val currentTableName: String = "",
        val completedLessons: Int = 0,    // 已上课程数
        val totalHours: Float = 0f,          // 累计小时
        val currentLanguageTag: String = "zh-Hans",
        val currentThemeMode: top.sakimidare.seutimetable.data.repository.UserPreferenceRepository.ThemeMode =
            top.sakimidare.seutimetable.data.repository.UserPreferenceRepository.ThemeMode.SYSTEM
    )
    data class Action(
        val icon: ImageVector,
        val labelRes: Int,
        val trailing: String? = null,
        val onClick: () -> Unit
    ) : ProfileItem()

    data class Toggle(
        val icon: ImageVector,
        val labelRes: Int,
        val isChecked: Boolean,
        val onToggle: (Boolean) -> Unit
    ) : ProfileItem()
}

data class ProfileSection(
    val titleRes: Int? = null,
    val items: List<ProfileItem>
)