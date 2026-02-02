package top.sakimidare.seutimetable.viewmodels


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.sakimidare.seutimetable.ui.main.MainTab

class MainViewModel : ViewModel() {
    // 初始页面设置为 Timetable
    private val _currentTab = MutableStateFlow<MainTab>(MainTab.Timetable)
    val currentTab = _currentTab.asStateFlow()

    fun updateTab(tab: MainTab) {
        _currentTab.value = tab
    }
}