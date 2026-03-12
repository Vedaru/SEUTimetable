package top.sakimidare.seutimetable

import android.app.Application
import android.content.Context

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
    }
}
