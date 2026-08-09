package app.rocat.settings

import android.content.Context
import android.net.Uri
import app.rocat.i18n.AppLanguage

/**
 * Thin persistence layer for app settings, backed by a private [android.content.SharedPreferences].
 * Stores the selected language and the main storage directory (as a SAF tree URI). A dedicated
 * repository keeps the value in a single place so both the i18n provider and the storage manager
 * observe the same source of truth.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var language: AppLanguage
        get() = runCatching { AppLanguage.valueOf(prefs.getString(KEY_LANGUAGE, null) ?: "") }
            .getOrDefault(AppLanguage.ENGLISH)
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value.name).apply()

    /** The SAF tree URI of the main storage folder, or null until the user picks one. */
    var storageUri: Uri?
        get() = prefs.getString(KEY_STORAGE_URI, null)?.let(Uri::parse)
        set(value) {
            val editor = prefs.edit()
            if (value == null) editor.remove(KEY_STORAGE_URI) else editor.putString(KEY_STORAGE_URI, value.toString())
            editor.apply()
        }

    val hasStorageDirectory: Boolean
        get() = storageUri != null

    companion object {
        private const val PREFS_NAME = "rocat_settings"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_STORAGE_URI = "storage_uri"
    }
}
