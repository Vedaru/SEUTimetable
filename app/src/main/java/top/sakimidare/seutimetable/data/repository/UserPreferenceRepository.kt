package top.sakimidare.seutimetable.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferenceRepository(private val context: Context) {

    private object PreferencesKeys {
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
    }

    /**
     * 观察免责声明是否已同意
     * 使用 Flow 可以让 UI 实时响应状态变化
     */
    val isDisclaimerAccepted: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // 默认值为 false，即未同意
            preferences[PreferencesKeys.DISCLAIMER_ACCEPTED] ?: false
        }

    /**
     * 更新同意状态
     */
    suspend fun updateDisclaimerAccepted(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DISCLAIMER_ACCEPTED] = accepted
        }
    }
}