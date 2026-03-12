package top.sakimidare.seutimetable.notifications

import android.content.Context

object NewsNotificationStore {
    private const val PREFS_NAME = "news_notifications"
    private const val KEY_PREFIX = "seen_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSeenUrls(context: Context, categoryPath: String): Set<String> {
        val key = KEY_PREFIX + categoryPath
        return prefs(context).getStringSet(key, emptySet()) ?: emptySet()
    }

    fun saveSeenUrls(context: Context, categoryPath: String, urls: Set<String>) {
        val key = KEY_PREFIX + categoryPath
        prefs(context).edit().putStringSet(key, urls).apply()
    }
}
