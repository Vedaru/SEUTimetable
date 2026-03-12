package top.sakimidare.seutimetable

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.lifecycleScope
import top.sakimidare.seutimetable.data.local.AppDatabase
import top.sakimidare.seutimetable.data.repository.CourseRepository
import top.sakimidare.seutimetable.data.repository.UserPreferenceRepository
import top.sakimidare.seutimetable.ui.main.MainScreen
import top.sakimidare.seutimetable.ui.theme.SEUTimetableTheme
import top.sakimidare.seutimetable.viewmodels.TimetableViewModel
import top.sakimidare.seutimetable.viewmodels.TimetableViewModelFactory
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Activity context must also reflect the persisted locale, otherwise
        // the first screen may render in the wrong language after a cold
        // start with an override in shared prefs.
        super.attachBaseContext(LocaleManager.applySavedLanguage(newBase))
    }
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val prefRepository = UserPreferenceRepository(applicationContext)
        val database = AppDatabase.getDatabase(applicationContext)
        val courseRepository = CourseRepository(
            courseDao = database.courseDao(),
            tableDao = database.tableDao(),
            context = applicationContext
        )

        val viewModel: TimetableViewModel by viewModels {
            TimetableViewModelFactory(courseRepository, prefRepository)
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            // observe theme preference inside Compose
            val themeMode by prefRepository.themeModeFlow.collectAsState(initial = UserPreferenceRepository.ThemeMode.SYSTEM)

            SEUTimetableTheme(themeMode = themeMode) {
                MainScreen(windowSizeClass, viewModel)
            }
        }
    }
}