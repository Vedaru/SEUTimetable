package top.sakimidare.seutimetable.ui.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import top.sakimidare.seutimetable.R
sealed class MainTab(
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Timetable : MainTab(R.string.timetable, Icons.Default.DateRange)
    object News : MainTab(R.string.news, Icons.Default.Info)
    object Profile : MainTab(R.string.me, Icons.Default.Person)
}
