package eu.kanade.tachiyomi.ui.reader.translate

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

enum class OcrLanguage {
    LATIN,
    JAPANESE,
    CHINESE,
    KOREAN,
}

class TextRecognitionHelper {

    // Menggunakan backing field nullable (?)
    private var _latinRecognizer: TextRecognizer? = null
    private val latinRecognizer: TextRecognizer
        get() {
            if (_latinRecognizer == null) {
                _latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            }
            return _latinRecognizer!!
        }
    
    private var _japaneseRecognizer: TextRecognizer? = null
    private val japaneseRecognizer: TextRecognizer
        get() {
            if (_japaneseRecognizer == null) {
                _japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            }
            return _japaneseRecognizer!!
        }
    
    private var _chineseRecognizer: TextRecognizer? = null
    private val chineseRecognizer: TextRecognizer
        get() {
            if (_chineseRecognizer == null) {
                _chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            }
            return _chineseRecognizer!!
        }
    
    private var _koreanRecognizer: TextRecognizer? = null
    private val koreanRecognizer: TextRecognizer
        get() {
            if (_koreanRecognizer == null) {
                _koreanRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            }
            return _koreanRecognizer!!
        }

    private fun getRecognizer(language: OcrLanguage): TextRecognizer {
        return when (language) {
            OcrLanguage.JAPANESE -> japaneseRecognizer
            OcrLanguage.CHINESE -> chineseRecognizer
            OcrLanguage.KOREAN -> koreanRecognizer
            OcrLanguage.LATIN -> latinRecognizer
        }
    }

    suspend fun recognize(
        bitmap: Bitmap,
        language: OcrLanguage = OcrLanguage.JAPANESE,
    ): List<OcrResult> = withContext(Dispatchers.IO) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val recognizer = getRecognizer(language)

        suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val results = visionText.textBlocks.flatMap { block ->
                        block.lines.map { line ->
                            OcrResult(
                                text = line.text,
                                boundingBox = line.boundingBox ?: block.boundingBox ?: Rect(),
                                confidence = null,
                            )
                        }
                    }
                    continuation.resume(results)
                }
                .addOnFailureListener { exception ->
                    continuation.resume(emptyList())
                }
        }
    }

    fun close() {
        // Hanya ditutup kalau sebelumnya pernah dipakai (nilainya tidak null)
        _latinRecognizer?.close()
        _japaneseRecognizer?.close()
        _chineseRecognizer?.close()
        _koreanRecognizer?.close()
    }
}
