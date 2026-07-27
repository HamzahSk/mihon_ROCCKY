package eu.kanade.tachiyomi.ui.reader.translate

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScreenCaptureHelper(
    private val mediaProjection: MediaProjection,
    private val defaultWidth: Int,
    private val defaultHeight: Int,
    private val defaultDensityDpi: Int,
) {
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    fun startCapture(
        captureWidth: Int = defaultWidth,
        captureHeight: Int = defaultHeight,
        captureDensityDpi: Int = defaultDensityDpi,
    ) {
        if (virtualDisplay != null) return
        imageReader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "OCR-ScreenCapture",
            captureWidth,
            captureHeight,
            captureDensityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            null,
        )
    }

    suspend fun captureBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        val reader = imageReader ?: return@withContext null
        reader.acquireLatestImage()?.use { image -> bitmapFromImage(image) }
    }

    private fun bitmapFromImage(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val imgWidth = image.width
        val imgHeight = image.height

        val bitmap = Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.ARGB_8888)

        if (rowStride == imgWidth * pixelStride) {
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)
        } else {
            val pixels = IntArray(imgWidth * imgHeight)
            buffer.rewind()
            for (row in 0 until imgHeight) {
                buffer.position(row * rowStride)
                for (col in 0 until imgWidth) {
                    val r = buffer.get().toInt() and 0xFF
                    val g = buffer.get().toInt() and 0xFF
                    val b = buffer.get().toInt() and 0xFF
                    val a = buffer.get().toInt() and 0xFF
                    pixels[row * imgWidth + col] = (a shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            bitmap.setPixels(pixels, 0, imgWidth, 0, 0, imgWidth, imgHeight)
        }

        return bitmap
    }

    fun release() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection.stop()
    }
}
