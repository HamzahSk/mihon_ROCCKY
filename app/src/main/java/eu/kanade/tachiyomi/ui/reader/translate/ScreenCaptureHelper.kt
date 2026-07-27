package eu.kanade.tachiyomi.ui.reader.translate

import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

class ScreenCaptureHelper(
    private val mediaProjection: MediaProjection,
    private val defaultWidth: Int,
    private val defaultHeight: Int,
    private val defaultDensityDpi: Int,
) {
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var callbackThread: HandlerThread? = null
    private var latestBitmap: Bitmap? = null

    var captureWidth: Int = defaultWidth
        private set
    var captureHeight: Int = defaultHeight
        private set

    fun startCapture(
        captureWidth: Int = defaultWidth,
        captureHeight: Int = defaultHeight,
        captureDensityDpi: Int = defaultDensityDpi,
    ) {
        if (virtualDisplay != null) {
            logcat { "ScreenCaptureHelper: startCapture called but already running" }
            return
        }

        this.captureWidth = captureWidth
        this.captureHeight = captureHeight

        callbackThread = HandlerThread("ScreenCapture-Callback").apply { start() }

        imageReader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            try {
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val bitmap = bitmapFromImage(image)
                image.close()

                latestBitmap?.recycle()
                latestBitmap = bitmap
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "Error in ImageReader listener" }
            }
        }, Handler(callbackThread!!.looper))

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "OCR-ScreenCapture",
            captureWidth,
            captureHeight,
            captureDensityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            object : VirtualDisplay.Callback() {
                override fun onPaused() {
                    logcat { "VirtualDisplay: paused" }
                }

                override fun onResumed() {
                    logcat { "VirtualDisplay: resumed" }
                }

                override fun onStopped() {
                    logcat { "VirtualDisplay: stopped" }
                }
            },
            Handler(callbackThread!!.looper),
        )

        logcat {
            "VirtualDisplay created: ${virtualDisplay != null} " +
                "(${captureWidth}x$captureHeight @ ${captureDensityDpi}dpi)"
        }
    }

    suspend fun captureBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        val bmp = latestBitmap
        if (bmp == null || bmp.isRecycled) {
            return@withContext null
        }
        bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, false)
    }

    private fun bitmapFromImage(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val imgWidth = image.width
        val imgHeight = image.height

        val bitmap = Bitmap.createBitmap(imgWidth, imgHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(imgWidth * imgHeight)
        
        buffer.rewind()
        for (row in 0 until imgHeight) {
            // Pindah ke posisi awal baris ini
            buffer.position(row * rowStride)
            for (col in 0 until imgWidth) {
                val r = buffer.get().toInt() and 0xFF
                val g = buffer.get().toInt() and 0xFF
                val b = buffer.get().toInt() and 0xFF
                
                // Kita abaikan alpha bawaan dari buffer karena sering kali 0 (transparan)
                buffer.get() 
                
                // PAKSA ALPHA MENJADI 255 (0xFF) AGAR GAMBAR SOLID
                val a = 0xFF 
                
                pixels[row * imgWidth + col] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        bitmap.setPixels(pixels, 0, imgWidth, 0, 0, imgWidth, imgHeight)

        return bitmap
    }

    fun release() {
        callbackThread?.quitSafely()
        callbackThread = null
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        latestBitmap?.recycle()
        latestBitmap = null
        mediaProjection.stop()
        logcat { "ScreenCaptureHelper: released" }
    }
}
