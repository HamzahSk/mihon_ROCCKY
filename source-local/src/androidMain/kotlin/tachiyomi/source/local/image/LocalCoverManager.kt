package tachiyomi.source.local.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.system.CoverTypeSetter
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.local.io.LocalSourceFileSystem
import java.io.InputStream

private const val DEFAULT_COVER_NAME = "cover.jpg"

actual class LocalCoverManager(
    private val context: Context,
    private val fileSystem: LocalSourceFileSystem,
) {

    actual fun find(mangaUrl: String): UniFile? {
        return fileSystem.getFilesInMangaDirectory(mangaUrl)
            // Get all file whose names start with "cover"
            .filter { it.isFile && it.nameWithoutExtension.equals("cover", ignoreCase = true) }
            // Get the first actual image
            .firstOrNull { ImageUtil.isImage(it.name) { it.openInputStream() } }
    }

    actual fun update(
        manga: SManga,
        inputStream: InputStream,
    ): UniFile? {
        val directory = fileSystem.getMangaDirectory(manga.url)
        if (directory == null) {
            inputStream.close()
            return null
        }

        val targetFile = find(manga.url) ?: directory.createFile(DEFAULT_COVER_NAME)!!

        inputStream.use { input ->
            targetFile.openOutputStream().use { output ->
                input.copyTo(output)
            }
        }

        DiskUtil.createNoMediaFile(directory, context)

        manga.thumbnail_url = targetFile.uri.toString()
        return targetFile
    }

    actual fun updateWithText(
        manga: SManga,
        inputStream: InputStream,
        title: String,
        lang: String?,
        chapterCount: String?,
    ): UniFile? {
        val directory = fileSystem.getMangaDirectory(manga.url)
        if (directory == null) {
            inputStream.close()
            return null
        }

        val targetFile = find(manga.url) ?: directory.createFile(DEFAULT_COVER_NAME)!!

        val bitmap = inputStream.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: return null

        val typesetBitmap = CoverTypeSetter.applyToCover(bitmap, title, lang, chapterCount)

        targetFile.openOutputStream().use { output ->
            typesetBitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }

        if (bitmap !== typesetBitmap) {
            bitmap.recycle()
        }
        typesetBitmap.recycle()

        DiskUtil.createNoMediaFile(directory, context)

        manga.thumbnail_url = targetFile.uri.toString()
        return targetFile
    }
}
