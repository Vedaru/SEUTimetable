package top.sakimidare.seutimetable

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import top.sakimidare.seutimetable.ui.main.MainTab
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import top.sakimidare.seutimetable.data.local.AppDatabase
import top.sakimidare.seutimetable.data.repository.CourseRepository
import top.sakimidare.seutimetable.data.repository.UserPreferenceRepository
import top.sakimidare.seutimetable.ui.main.MainScreen
import top.sakimidare.seutimetable.ui.theme.SEUTimetableTheme
import top.sakimidare.seutimetable.viewmodels.TimetableViewModel
import top.sakimidare.seutimetable.viewmodels.TimetableViewModelFactory
import top.sakimidare.seutimetable.viewmodels.MainViewModel
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_TAB = "extra_target_tab"
        const val TAB_NEWS = "news"
    }

    private var pendingTargetTab by mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no action needed */ }

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

        // request notification permission on Android13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        pendingTargetTab = intent?.getStringExtra(EXTRA_TARGET_TAB)

        val prefRepository = UserPreferenceRepository(applicationContext)
        val database = AppDatabase.getDatabase(applicationContext)
        val courseRepository = CourseRepository(
            courseDao = database.courseDao(),
            tableDao = database.tableDao(),
            context = applicationContext
        )

        val timetableViewModel: TimetableViewModel by viewModels {
            TimetableViewModelFactory(courseRepository, prefRepository)
        }
        val mainViewModel: MainViewModel by viewModels()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            // observe theme preference inside Compose
            val themeMode by prefRepository.themeModeFlow.collectAsState(initial = UserPreferenceRepository.ThemeMode.SYSTEM)
            val target = pendingTargetTab
            LaunchedEffect(target) {
                if (target == TAB_NEWS) {
                    mainViewModel.updateTab(MainTab.News)
                }
            }

            SEUTimetableTheme(themeMode = themeMode) {
                MainScreen(windowSizeClass, timetableViewModel, mainViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTargetTab = intent.getStringExtra(EXTRA_TARGET_TAB)
    }
}