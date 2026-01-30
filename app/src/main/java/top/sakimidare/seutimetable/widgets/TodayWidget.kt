package top.sakimidare.seutimetable.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
// import androidx.compose.ui.res.stringResource
// 不要用这个，会闪退
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import top.sakimidare.seutimetable.MainActivity
import top.sakimidare.seutimetable.R
import top.sakimidare.seutimetable.data.local.AppDatabase
import top.sakimidare.seutimetable.data.model.Course
import top.sakimidare.seutimetable.data.model.SemesterConfig
import top.sakimidare.seutimetable.data.model.TableMetadata
import top.sakimidare.seutimetable.data.model.matches
import top.sakimidare.seutimetable.data.utils.TimetableUtils.calculateCurrentWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.jvm.java

val FORCE_REFRESH_KEY = longPreferencesKey("force_refresh")

class TodayWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    private suspend fun fetchFilteredData(context: Context): Pair<TableMetadata, List<Course>>? {
        return try {
            val db = AppDatabase.getDatabase(context)
            val currentTable = db.tableDao().getCurrentTableSync() ?: return null

            val currentWeek = calculateCurrentWeek(currentTable.semesterConfig) ?: return null
            val today = LocalDate.now().dayOfWeek

            val courses = db.courseDao().getCoursesByDaySync(currentTable.id, today)
                .filter { it.weekRule.matches(currentWeek) }

            currentTable to courses // 返回 Pair
        } catch (e: Exception) {
            Log.e("WidgetFlow", "3.1 [FORCE_LOG] fetchFilteredData ERROR", e)
            null
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d("WidgetFlow", "3. [FORCE_LOG] provideGlance ENTERED")

        // ✅ 在提供内容前预取一次数据，确保首次加载不白屏
        val initialData = fetchFilteredData(context)

        provideContent {
            // 1. 订阅 DataStore 状态
            val prefs = currentState<Preferences>()
            val ts = prefs[FORCE_REFRESH_KEY] ?: 0L

            // 2. 状态现在持有整个数据对。只要 ts 变了，就去拉新的 Table 和 Courses
            val dataPair by produceState(initialValue = initialData, key1 = ts) {
                val updated = fetchFilteredData(context)
                Log.d("WidgetFlow", "3.1 [RE-FETCH] ts changed")
                value = updated
            }

            // 如果 dataPair 为 null，说明没课表或者不在学期内
            val currentTable = dataPair?.first
            val courses = dataPair?.second ?: emptyList()

            if (currentTable != null) {
                // 顺利拿到 table，把配置和课程一起传进去
                TodayWidgetContent(courses, currentTable)
            } else {
                // 兜底显示空状态（比如没设当前课表时）
                EmptyState()
            }
        }
    }

    @Composable
    private fun TodayWidgetContent(courses: List<Course>, currentTable: TableMetadata) {
        GlanceTheme {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .background(GlanceTheme.colors.widgetBackground)
                    .cornerRadius(16.dp)
                    .padding(12.dp)
            ) {
                // 顶部标题
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        // text = stringResource(R.string.widget_today_desc),
                        text = LocalContext.current.getString(R.string.widget_today_desc),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .withLocale(Locale.getDefault())
                    Text(
                        text = LocalDate.now().format(dateFormatter),
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }

                Spacer(GlanceModifier.height(10.dp))

                if (courses.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                        items(courses) { course ->
                            CourseItem(
                                course, currentTable.semesterConfig
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EmptyState() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(16.dp)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "☕", style = TextStyle(fontSize = 24.sp))
            Text(
                text = LocalContext.current.getString(R.string.no_courses_today),
                style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
        }
    }

    @Composable
    private fun CourseItem(course: Course, config: SemesterConfig) {
        val startTime = config.periods.getOrNull(course.startPeriod - 1)?.start?.toString() ?: ""
        val endTime =
            config.periods.getOrNull(course.startPeriod + course.duration - 2)?.end?.toString()
                ?: ""

        Box(
            modifier = GlanceModifier.padding(vertical = 8.dp)
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.secondaryContainer)
                    .cornerRadius(12.dp)
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 左侧：时间/节次栏
                Column(
                    modifier = GlanceModifier.width(55.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = startTime,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSecondaryContainer
                        )
                    )
                    Text(
                        text = endTime,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    // 保留节次的小标签
                    Text(
                        text = LocalContext.current.getString(
                            R.string.section_format,
                            course.startPeriod,
                            course.startPeriod + course.duration - 1
                        ),
                        style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.primary)
                    )
                }

                // 2. 分割线
                Box(
                    modifier = GlanceModifier
                        .width(1.dp)
                        .height(35.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                        .padding(horizontal = 8.dp)
                ) {}

                // 3. 右侧：详情
                Column(modifier = GlanceModifier.defaultWeight().padding(start = 12.dp)) {
                    Text(
                        text = course.name,
                        maxLines = 1,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GlanceTheme.colors.onSecondaryContainer
                        )
                    )

                    if (course.location.isNotBlank()) {
                        Text(
                            text = "@${course.location}",
                            maxLines = 1,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

fun requestPinTodayWidget(context: Context) {
    Log.d("WidgetFlow", "2. [FORCE_LOG] requestPinTodayWidget ENTERED")
    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
    val myProvider = ComponentName(context, TodayWidgetReceiver::class.java)

    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        Log.d("WidgetFlow", "2.1 [FORCE_LOG] requestPinTodayWidget SUPPORTED")
        // 创建一个点击确认后的回调（可选）
        val successCallback = PendingIntent.getBroadcast(
            context, 0, Intent(context, TodayWidgetReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // 弹出系统级对话框
        appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
    } else {
        Toast.makeText(context,
            context.getString(R.string.no_shortcut_for_creating_widgets), Toast.LENGTH_SHORT).show()
    }
}