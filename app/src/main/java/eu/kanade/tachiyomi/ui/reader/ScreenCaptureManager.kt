package eu.kanade.tachiyomi.ui.reader

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

class ScreenCaptureManager(
    private val mediaProjectionManager: MediaProjectionManager,
) {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    fun createCaptureIntent(): android.content.Intent {
        return mediaProjectionManager.createScreenCaptureIntent()
    }

    fun startCapture(
        resultCode: Int,
        data: android.content.Intent,
        densityDpi: Int,
        width: Int,
        height: Int,
        onBitmapCaptured: (Bitmap) -> Unit,
    ) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "OCR Screen Capture",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null,
        )

        val handler = Handler(Looper.getMainLooper())
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                try {
                    val bitmap = imageToBitmap(image)
                    onBitmapCaptured(bitmap)
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Failed to convert image to bitmap" }
                } finally {
                    image.close()
                    release()
                }
            }
        }, handler)
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val width = image.width
        val height = image.height

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        buffer.rewind()
        for (row in 0 until height) {
            val rowOffset = row * rowStride
            for (col in 0 until width) {
                val pixelOffset = rowOffset + col * pixelStride
                val r = buffer.get(pixelOffset).toInt() and 0xFF
                val g = buffer.get(pixelOffset + 1).toInt() and 0xFF
                val b = buffer.get(pixelOffset + 2).toInt() and 0xFF
                val a = buffer.get(pixelOffset + 3).toInt() and 0xFF
                pixels[row * width + col] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    fun isActive(): Boolean {
        return virtualDisplay != null
    }

    fun release() {
        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "Error releasing VirtualDisplay: ${e.message}" }
        }
        virtualDisplay = null
        try {
            imageReader?.close()
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "Error closing ImageReader: ${e.message}" }
        }
        imageReader = null
        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            logcat(LogPriority.WARN) { "Error stopping MediaProjection: ${e.message}" }
        }
        mediaProjection = null
    }
}
