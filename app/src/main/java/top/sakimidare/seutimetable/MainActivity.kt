package top.sakimidare.seutimetable

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import top.sakimidare.seutimetable.data.local.AppDatabase
import top.sakimidare.seutimetable.data.repository.CourseRepository
import top.sakimidare.seutimetable.data.repository.UserPreferenceRepository
import top.sakimidare.seutimetable.ui.main.MainScreen
import top.sakimidare.seutimetable.ui.theme.SEUTimetableTheme
import top.sakimidare.seutimetable.viewmodels.TimetableViewModel
import top.sakimidare.seutimetable.viewmodels.TimetableViewModelFactory

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MainActivity", "🚀 onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. 初始化数据库与持久化仓库
        val database = AppDatabase.getDatabase(applicationContext)
        val prefRepository = UserPreferenceRepository(applicationContext)

        // 2. 初始化核心业务 Repository
        val courseRepository = CourseRepository(
            courseDao = database.courseDao(),
            tableDao = database.tableDao(),
            context = applicationContext
        )

        // 3. 使用 Factory 创建 ViewModel
        val viewModel: TimetableViewModel by viewModels {
            TimetableViewModelFactory(
                courseRepository = courseRepository,
                prefRepository = prefRepository
            )
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)

            SEUTimetableTheme {
                MainScreen(
                    timetableViewModel = viewModel,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }
}