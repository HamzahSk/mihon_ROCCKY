package tachiyomi.source.local.image

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SManga
import java.io.InputStream

expect class LocalCoverManager {

    fun find(mangaUrl: String): UniFile?

    fun update(manga: SManga, inputStream: InputStream): UniFile?

    fun updateWithText(
        manga: SManga,
        inputStream: InputStream,
        title: String,
        lang: String? = null,
        chapterCount: String? = null,
    ): UniFile?
}
