package eu.kanade.tachiyomi.ui.reader.translate

import android.graphics.Rect

data class OcrResult(
    val text: String,
    val boundingBox: Rect,
    val confidence: Float? = null,
)

data class OcrOverlay(
    val results: List<OcrResult>,
    val imageWidth: Int,
    val imageHeight: Int,
)
