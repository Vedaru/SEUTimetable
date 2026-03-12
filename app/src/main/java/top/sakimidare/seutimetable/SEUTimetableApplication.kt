package top.sakimidare.seutimetable

import android.app.Application
import android.content.Context
import top.sakimidare.seutimetable.notifications.NotificationHelper
import top.sakimidare.seutimetable.notifications.NewsUpdateWorker

/**
 * Application subclass.  No special language handling needed since the
 * app always follows the system locale.
 */
class SEUTimetableApplication : Application() {
    override fun attachBaseContext(base: Context) {
        // ensure stored override (if any) is applied before any UI loads
        super.attachBaseContext(LocaleManager.applySavedLanguage(base))
    }

    override fun onCreate() {
        super.onCreate()
        // also make sure the application context has the locale applied; this
        // is primarily needed when components fetch resources directly from
        // applicationContext during startup (widgets, etc.).
        // no language overrides to apply

        // create notification channel immediately so the worker can post later
        NotificationHelper.createNotificationChannel(this)
        // schedule background job to check for news updates
        NewsUpdateWorker.schedule(this)
    }
}
