package eu.kanade.tachiyomi.ui.reader.translate

import android.graphics.Bitmap
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import kotlin.coroutines.resume

class OcrEngineImpl : MangaTranslatorEngine {

    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun detectText(bitmap: Bitmap): List<OcrResult> = withContext(Dispatchers.IO) {
        val image = InputImage.fromBitmap(bitmap, 0)
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val results = visionText.textBlocks.flatMap { block ->
                        block.lines.map { line ->
                            val box = line.boundingBox ?: return@map null
                            OcrResult(
                                text = line.text,
                                boundingBox = RectF(
                                    box.left.toFloat(),
                                    box.top.toFloat(),
                                    box.right.toFloat(),
                                    box.bottom.toFloat(),
                                ),
                                confidence = 0f,
                            )
                        }
                    }.filterNotNull()
                    continuation.resume(results)
                }
                .addOnFailureListener { e ->
                    logcat(LogPriority.ERROR, e) { "OCR detection failed" }
                    continuation.resume(emptyList())
                }
                .addOnCanceledListener {
                    continuation.resume(emptyList())
                }
        }
    }

    override fun release() {
        recognizer.close()
    }
}
