package eu.kanade.tachiyomi.ui.reader.translate

import android.graphics.Bitmap

interface MangaTranslatorEngine {

    suspend fun detectText(bitmap: Bitmap): List<OcrResult>

    fun release()
}
