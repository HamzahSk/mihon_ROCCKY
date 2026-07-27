package eu.kanade.tachiyomi.ui.reader.translate

import android.graphics.Bitmap

/**
 * Abstraction for a manga text processing engine.
 *
 * Stage 1 returns raw OCR text without translation.
 * Future stages will plug in translation APIs (Google Translate, DeepL, …).
 *
 * Implementations MUST be thread-safe and run their work on [Dispatchers.Default].
 */
interface MangaTranslatorEngine {

    /**
     * Run OCR on [bitmap] and return detected text blocks.
     *
     * @param bitmap the page image to analyse
     * @return list of detected text regions with bounding boxes and recognised text
     */
    suspend fun detectText(bitmap: Bitmap): List<OcrTextBlock>
}

/**
 * A single block of recognised text from OCR.
 *
 * @param text       the raw recognised string
 * @param boundingBox pixel coordinates on the source bitmap — left, top, right, bottom
 */
data class OcrTextBlock(
    val text: String,
    val boundingBox: android.graphics.RectF,
)
