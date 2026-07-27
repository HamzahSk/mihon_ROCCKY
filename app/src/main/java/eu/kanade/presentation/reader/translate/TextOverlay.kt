package eu.kanade.presentation.reader.translate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import eu.kanade.tachiyomi.ui.reader.translate.OcrResult

@Composable
fun TextOverlay(
    results: List<OcrResult>,
    bitmapWidth: Float,
    bitmapHeight: Float,
    modifier: Modifier = Modifier,
) {
    if (results.isEmpty()) return

    Canvas(
        modifier = modifier.fillMaxSize(),
    ) {
        val scaleX = size.width / maxOf(bitmapWidth, 1f)
        val scaleY = size.height / maxOf(bitmapHeight, 1f)

        for (result in results) {
            val left = result.boundingBox.left * scaleX
            val top = result.boundingBox.top * scaleY
            val right = result.boundingBox.right * scaleX
            val bottom = result.boundingBox.bottom * scaleY

            drawRect(
                color = Color(0x332299FF),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
            )

            drawRect(
                color = Color(0xFF2299FF),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 2f),
            )
        }
    }
}
