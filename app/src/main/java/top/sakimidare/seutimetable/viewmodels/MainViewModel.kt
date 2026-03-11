package top.sakimidare.seutimetable.viewmodels


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import top.sakimidare.seutimetable.ui.main.MainTab

class MainViewModel : ViewModel() {
    // 初始页面设置为 Today
    private val _currentTab = MutableStateFlow<MainTab>(MainTab.Today)
    val currentTab = _currentTab.asStateFlow()

    fun updateTab(tab: MainTab) {
        _currentTab.value = tab
    }
}