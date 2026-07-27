package eu.kanade.tachiyomi.ui.reader.translate

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import tachiyomi.core.common.util.system.logcat
import kotlin.coroutines.resume

/**
 * Google ML Kit–based OCR engine.
 *
 * Runs [TextRecognition] on a bitmap and returns the raw text blocks.
 * All heavy work is dispatched to [Dispatchers.Default].
 */
class OcrProcessor : MangaTranslatorEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun detectText(bitmap: Bitmap): List<OcrTextBlock> = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        try {
            val text = suspendCancellableCoroutine<com.google.mlkit.vision.text.Text?> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        cont.resume(result)
                    }
                    .addOnFailureListener {
                        cont.resume(null)
                    }
            }
            text?.textBlocks?.mapNotNull { block ->
                val box = block.boundingBox ?: return@mapNotNull null
                OcrTextBlock(
                    text = block.text,
                    boundingBox = RectF(box),
                )
            } ?: emptyList()
        } catch (e: Exception) {
            logcat { "OCR detection failed: ${e.message}" }
            emptyList()
        }
    }

    /**
     * Release the underlying ML Kit recognizer.
     */
    fun dispose() {
        recognizer.close()
    }
}
