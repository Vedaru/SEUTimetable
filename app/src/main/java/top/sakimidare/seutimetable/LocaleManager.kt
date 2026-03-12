package top.sakimidare.seutimetable

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.res.Resources
import java.util.Locale

/**
 * Helper object for managing the application locale at runtime.  
 *
 * The app stores the selected language in shared preferences and applies
 * it to the resources configuration whenever the language is changed.  
 * A restart of the app is required for the change to take full effect,
 * therefore callers should usually invoke [restartApp].
 */
object LocaleManager {

    private const val PREF_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "language"

    enum class Language(val locale: Locale?) {
        SYSTEM(null),
        ENGLISH(Locale.ENGLISH),
        SIMPLIFIED_CHINESE(Locale.SIMPLIFIED_CHINESE),
        JAPANESE(Locale.JAPANESE),
        SPANISH(Locale("es"));

        /**
         * Human‐readable name for this language, taken from string resources.
         */
        fun displayName(context: Context): String {
            return when (this) {
                SYSTEM -> context.getString(R.string.follow_system)
                ENGLISH -> context.getString(R.string.lang_english)
                SIMPLIFIED_CHINESE -> context.getString(R.string.lang_simplified_chinese)
                JAPANESE -> context.getString(R.string.lang_japanese)
                SPANISH -> context.getString(R.string.lang_spanish)
            }
        }
    }

    /**
     * Return the language value that is stored in shared preferences.
     *
     * This simply reads the user preference and converts it into the
     * [Language] enum.  It does **not** take into account the locale that the
     * application has currently applied to its resources – i.e. the stored
     * value is returned verbatim even if the app has overridden the system
     * locale for its own context.
     *
     * This helper exists because some callers (e.g. the settings screen) need
     * to display the raw preference rather than the resolved locale.
     */
    fun getSavedLanguage(context: Context): Language {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_LANGUAGE, null)
        return try {
            if (name == null) Language.SYSTEM else Language.valueOf(name)
        } catch (e: IllegalArgumentException) {
            Language.SYSTEM
        }
    }

    /**
     * Convenience alias for [getSavedLanguage].  kept for compatibility with
     * previous versions of the API and existing callers that already use
     * `getLanguage`.
     */
    fun getLanguage(context: Context): Language = getSavedLanguage(context)

    /**
     * Save the chosen language and immediately apply it.  If the language is
     * [Language.SYSTEM] we use the device's current locale rather than whatever
     * value may have been stored earlier by a previous manual selection.  This
     * prevents the "stuck on old language" issue described by the user.
     */
    fun setLanguage(context: Context, language: Language) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.name)
            .apply()
        // apply the new locale on the application context so that the
        // change takes effect everywhere; the caller may still recreate the
        // current activity to refresh its UI immediately.
        updateLocale(context.applicationContext, language)
    }

    private fun updateLocale(context: Context, language: Language) {
        val locale = when (language) {
            Language.SYSTEM -> getSystemLocale()
            else -> language.locale ?: getSystemLocale()
        }
        Locale.setDefault(locale)
        val res = context.resources
        val config = res.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        res.updateConfiguration(config, res.displayMetrics)
    }

    /**
     * Obtain the *actual* system locale from the system resources.  This is
     * independent of whatever value [Locale.getDefault] currently holds, which
     * may have been overridden by the app previously.  It is exposed publicly
     * because callers may want to display or inspect the current system
     * language (e.g. in the settings screen).  The method itself is
     * implementation‑detail‑free and safe.
     */
    fun getSystemLocale(): Locale {
        val config = Resources.getSystem().configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            config.locale
        }
    }

    /**
     * Apply the language saved in shared preferences to the supplied context
     * and return a context that uses the updated configuration.  This is
     * intended for use from [Application.attachBaseContext] and
     * [Activity.attachBaseContext] so that resources inflated later will be in
     * the correct locale.
     */
    fun applySavedLanguage(base: Context): Context {
        val language = getLanguage(base)
        val locale = when (language) {
            Language.SYSTEM -> getSystemLocale()
            else -> language.locale ?: getSystemLocale()
        }
        val config = base.resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        // also set the global default so later calls to Locale.getDefault()
        // are consistent with what the context is using.
        Locale.setDefault(locale)
        return base.createConfigurationContext(config)
    }

    /**
     * Restart the application by launching the main activity and finishing the
     * current one.  If the provided context is not an [Activity] nothing happens.
     */
    fun restartApp(context: Context) {
        if (context is Activity) {
            // Instead of killing the entire task we can simply recreate the
            // current activity; the caller can use this to apply a locale
            // change while staying on the same screen.  The existing logic is
            // retained as a fallback when a full restart is actually desired.
            try {
                context.recreate()
            } catch (_: Exception) {
                // if recreate fails for any reason, fall back to the previous
                // behaviour which launches the main activity and clears the
                // task.  This is unlikely but safer than crashing.
                val intent = context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                context.finish()
            }
        }
    }
}
