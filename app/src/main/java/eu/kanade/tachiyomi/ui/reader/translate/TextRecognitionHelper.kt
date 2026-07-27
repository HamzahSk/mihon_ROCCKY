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

    // Gunakan "by lazy" agar tidak langsung crash saat inisialisasi awal
    private val latinRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    
    private val japaneseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }
    
    private val chineseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }
    
    private val koreanRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
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
        // Kita juga bisa mengecek apakah variabelnya sudah diinisialisasi atau belum
        // untuk menghindari crash saat menutup aplikasi
        latinRecognizer.close()
        japaneseRecognizer.close()
        chineseRecognizer.close()
        koreanRecognizer.close()
    }
}
