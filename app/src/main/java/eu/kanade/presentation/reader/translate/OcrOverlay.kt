package eu.kanade.presentation.reader.translate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.ui.reader.translate.OcrTextBlock

/**
 * Semi-transparent overlay that draws bounding boxes and recognised text
 * on top of the manga page image.
 *
 * Coordinates are expressed as fractions [0..1] of the image dimensions
 * so they align regardless of scale/zoom.
 */
@Composable
fun OcrOverlay(
    textBlocks: List<OcrTextBlock>,
    modifier: Modifier = Modifier,
    imageWidth: Float = 1f,
    imageHeight: Float = 1f,
) {
    if (textBlocks.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        val scaleX = size.width / imageWidth
        val scaleY = size.height / imageHeight

        for (block in textBlocks) {
            val left = block.boundingBox.left * scaleX
            val top = block.boundingBox.top * scaleY
            val right = block.boundingBox.right * scaleX
            val bottom = block.boundingBox.bottom * scaleY
            val boxWidth = right - left
            val boxHeight = bottom - top

            // Draw bounding box
            drawRect(
                color = Color(0xCC00FF00),
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                style = Stroke(width = 2f),
            )

            // Fill with semi-transparent background
            drawRect(
                color = Color(0x2200FF00),
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
            )

            // Draw text label above the box
            val textLayoutResult = textMeasurer.measure(
                text = block.text,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    background = Color(0xAA000000),
                ),
                maxLines = 1,
            )

            // Position label slightly above the bounding box
            val labelX = left
            val labelY = (top - textLayoutResult.size.height).coerceAtLeast(0f)

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(labelX, labelY),
            )
        }
    }
}