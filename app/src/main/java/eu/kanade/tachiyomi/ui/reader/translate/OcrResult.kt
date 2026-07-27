package eu.kanade.tachiyomi.ui.reader.translate

import android.graphics.RectF

data class OcrResult(
    val text: String,
    val boundingBox: RectF,
    val confidence: Float = 0f,
)

data class OcrRegion(
    val pageIndex: Int,
    val results: List<OcrResult>,
)
