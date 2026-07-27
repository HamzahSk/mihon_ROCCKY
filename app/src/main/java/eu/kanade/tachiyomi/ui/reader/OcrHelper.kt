package eu.kanade.tachiyomi.ui.reader

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import kotlin.coroutines.resume

class OcrHelper {

    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(bitmap: Bitmap): Result<String> {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val extractedText = visionText.text
                    continuation.resume(Result.success(extractedText))
                }
                .addOnFailureListener { e ->
                    logcat(LogPriority.ERROR, e) { "Text recognition failed" }
                    continuation.resume(Result.failure(e))
                }
        }
    }

    fun close() {
        recognizer.close()
    }
}
