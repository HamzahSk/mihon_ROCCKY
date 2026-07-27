package eu.kanade.tachiyomi.ui.reader.translate

interface MangaTranslatorEngine {
    suspend fun translate(
        text: String,
        sourceLang: String = "auto",
        targetLang: String = "en",
    ): String
}
