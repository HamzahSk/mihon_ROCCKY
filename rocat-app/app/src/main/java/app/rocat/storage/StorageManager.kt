package app.rocat.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import app.rocat.settings.SettingsRepository
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owns the app's Storage Access Framework integration and the scrape folder layout.
 *
 * The user grants access to a single main directory via `ACTION_OPEN_DOCUMENT_TREE`; the
 * chosen URI is persisted (with a persistable grant) in [SettingsRepository]. Every scrape
 * writes into a dedicated sub-folder at `[MainDirectory]/Scrapes/[scrapeId]/` so results
 * stay organized and isolated per scrape run.
 */
class StorageManager(
    private val context: Context,
    private val settings: SettingsRepository,
) {

    /** Whether the user has picked (and we hold a grant for) a main directory. */
    val isConfigured: Boolean
        get() = settings.hasStorageDirectory

    /** The persisted main directory URI, if any. */
    val mainUri: Uri?
        get() = settings.storageUri

    /**
     * Persists the read/write grant for [uri] returned by the folder picker. Mirrors how
     * mihon stores its "download location" tree URI. Returns false when the system refused
     * to keep the permission.
     */
    fun takePersistablePermission(uri: Uri): Boolean = runCatching {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        settings.storageUri = uri
        true
    }.getOrDefault(false)

    /** Replaces the main directory with [uri] (called when the user changes it in Settings). */
    fun setMainDirectory(uri: Uri) {
        settings.storageUri = uri
    }

    /** Clears the current main directory reference. */
    fun clearMainDirectory() {
        settings.storageUri = null
    }

    /** The [DocumentFile] for the main directory, or null when not configured. */
    fun mainDocument(): DocumentFile? = mainUri?.let { DocumentFile.fromTreeUri(context, it) }

    /** Human-readable name of the main directory (e.g. "Downloads"), for the Settings UI. */
    fun mainDirectoryName(): String {
        val name = mainUri?.let { DocumentFile.fromTreeUri(context, it)?.name }
        return name?.takeIf { it.isNotBlank() } ?: mainUri?.lastPathSegment?.substringAfterLast(':') ?: ""
    }

    /**
     * Creates (or reuses) the per-scrape folder `[MainDirectory]/Scrapes/[name]/`. All files
     * belonging to one scrape run should be written under the returned [DocumentFile].
     */
    fun createScrapeFolder(name: String): DocumentFile? {
        val root = mainDocument() ?: return null
        val scrapes = root.findFile(SCRAPES_DIR) ?: root.createDirectory(SCRAPES_DIR) ?: return null
        return scrapes.findFile(name) ?: scrapes.createDirectory(name)
    }

    /**
     * Empties the Coil image cache (memory + disk) and every file under `context.cacheDir`.
     * Runs on a background dispatcher because disk eviction can take a moment.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        val imageLoader = runCatching { SingletonImageLoader.get(context) }.getOrNull()
        runCatching { imageLoader?.memoryCache?.clear() }
        runCatching { imageLoader?.diskCache?.clear() }
        runCatching { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
    }

    companion object {
        /** Top-level folder holding every scrape run. */
        const val SCRAPES_DIR = "Scrapes"
    }
}
