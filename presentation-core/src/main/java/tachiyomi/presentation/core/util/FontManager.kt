package tachiyomi.presentation.core.util

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.File
import java.io.FileOutputStream

/**
 * Manages custom font files: persistence, loading, and cleanup.
 */
object FontManager {

    private const val FONT_DIR = "custom_fonts"
    private const val FONT_FILE_NAME = "custom_font.ttf"

    /**
     * Returns the directory where custom fonts are stored.
     */
    private fun getFontDir(context: Context): File {
        return File(context.filesDir, FONT_DIR).apply { mkdirs() }
    }

    /**
     * Returns the file where the active custom font is stored.
     */
    fun getFontFile(context: Context): File {
        return File(getFontDir(context), FONT_FILE_NAME)
    }

    /**
     * Saves a font from the given [uri] to internal storage.
     * Returns true on success, false otherwise.
     */
    fun saveFont(context: Context, uri: Uri): Boolean {
        return try {
            val fontFile = getFontFile(context)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(fontFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return false
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to save font from URI: $uri" }
            false
        }
    }

    /**
     * Deletes the saved custom font file.
     */
    fun clearFont(context: Context) {
        getFontFile(context).delete()
    }

    /**
     * Returns true if a custom font file exists.
     */
    fun hasCustomFont(context: Context): Boolean {
        return getFontFile(context).exists()
    }

    /**
     * Returns a [FontFamily] for the saved custom font, or null if none exists
     * or the file is invalid.
     */
    fun getFontFamily(context: Context): FontFamily? {
        val fontFile = getFontFile(context)
        if (!fontFile.exists()) return null
        return try {
            FontFamily(
                Font(
                    file = fontFile,
                    weight = FontWeight.Normal,
                    style = FontStyle.Normal,
                ),
                Font(
                    file = fontFile,
                    weight = FontWeight.Medium,
                    style = FontStyle.Normal,
                ),
                Font(
                    file = fontFile,
                    weight = FontWeight.Bold,
                    style = FontStyle.Normal,
                ),
                Font(
                    file = fontFile,
                    weight = FontWeight.SemiBold,
                    style = FontStyle.Normal,
                ),
            )
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to create FontFamily from file: ${fontFile.path}" }
            null
        }
    }

    /**
     * Returns a [Typeface] for the saved custom font, or null if none exists.
     */
    fun getTypeface(context: Context): Typeface? {
        val fontFile = getFontFile(context)
        if (!fontFile.exists()) return null
        return try {
            Typeface.createFromFile(fontFile)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to create Typeface from file: ${fontFile.path}" }
            null
        }
    }

    /**
     * Returns the display name of the font file from a [uri], or null if unavailable.
     */
    fun getFontFileName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    uri.lastPathSegment
                }
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }

    /**
     * Validates that the URI points to a file with a supported font extension.
     */
    fun isValidFontUri(context: Context, uri: Uri): Boolean {
        val name = getFontFileName(context, uri) ?: return false
        val lower = name.lowercase()
        return lower.endsWith(".ttf") || lower.endsWith(".otf")
    }
}
