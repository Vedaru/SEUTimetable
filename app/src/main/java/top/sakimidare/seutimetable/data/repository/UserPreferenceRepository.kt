package top.sakimidare.seutimetable.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale
import top.sakimidare.seutimetable.R

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferenceRepository(private val context: Context) {

    // language switching removed; application will always follow system locale

    private object PreferencesKeys {
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")

        // --- 新增：全局显示设置的 Key ---
        val SHOW_TIMELINE = booleanPreferencesKey("show_timeline")       // 是否显示侧边节次轴
        val SHOW_DATE_HEADER = booleanPreferencesKey("show_date_header") // 是否显示表头日期
        val SHOW_PERIOD_TIME = booleanPreferencesKey("show_period_time") // 是否显示具体时间点
        val SHOW_NON_CURRENT_WEEK = booleanPreferencesKey("show_non_current_week")
        val NEWS_NOTIFICATIONS = booleanPreferencesKey("news_notifications")
        val THEME_MODE = androidx.datastore.preferences.core.intPreferencesKey("theme_mode")
    }

    // 基础的 DataStore 读取错误处理封装
    private val dataFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    /** 观察免责声明状态 */
    val isDisclaimerAccepted: Flow<Boolean> = dataFlow.map { it[PreferencesKeys.DISCLAIMER_ACCEPTED] ?: false }

    suspend fun updateDisclaimerAccepted(accepted: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DISCLAIMER_ACCEPTED] = accepted }
    }

    // language preferences removed; nothing stored or observed

    /* -------------------------------------------------------
       2. 全局显示设置 (新增)
    ------------------------------------------------------- */

    /* -------------------------------------------------------
       3. 主题模式
       0 = Follow system (default), 1 = Light, 2 = Dark
    ------------------------------------------------------- */
    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    val newsNotificationFlow: Flow<Boolean> = dataFlow.map {
        // default to enabled so users receive updates after granting permission
        it[PreferencesKeys.NEWS_NOTIFICATIONS] ?: true
    }

    val themeModeFlow: Flow<ThemeMode> = dataFlow.map {
        when (it[PreferencesKeys.THEME_MODE] ?: 0) {
            1 -> ThemeMode.LIGHT
            2 -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    suspend fun getThemeMode(): ThemeMode {
        return try {
            when (context.dataStore.data.first()[PreferencesKeys.THEME_MODE] ?: 0) {
                1 -> ThemeMode.LIGHT
                2 -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = when (mode) {
            ThemeMode.LIGHT -> 1
            ThemeMode.DARK -> 2
            ThemeMode.SYSTEM -> 0
        } }
    }

    // 建议这样写读取逻辑，确保每次都是从最新的 data 中 map 出来的
    val showTimelineFlow: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[PreferencesKeys.SHOW_TIMELINE] ?: true }

    val showDateFlow: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[PreferencesKeys.SHOW_DATE_HEADER] ?: true }

    val showPeriodTimeFlow: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[PreferencesKeys.SHOW_PERIOD_TIME] ?: true }
    suspend fun updateShowTimeline(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_TIMELINE] = show }
    }
    suspend fun updateShowDate(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_DATE_HEADER] = show }
    }
    suspend fun updateShowPeriodTime(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_PERIOD_TIME] = show }
    }
    // 定义 Flow (默认设为 true，方便用户看到所有课程)
    val showNonCurrentWeekFlow: Flow<Boolean> = dataFlow.map {
        it[PreferencesKeys.SHOW_NON_CURRENT_WEEK] ?: true
    }

    // 定义更新方法
    suspend fun updateShowNonCurrentWeek(show: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_NON_CURRENT_WEEK] = show }
    }

    suspend fun updateNewsNotifications(enable: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.NEWS_NOTIFICATIONS] = enable }
    }
}

data class DisplayPreferences(
    val showTimeline: Boolean = true,
    val showDate: Boolean = true,
    val showPeriodTime: Boolean = true,
    val showNonCurrentWeek: Boolean = true
)
