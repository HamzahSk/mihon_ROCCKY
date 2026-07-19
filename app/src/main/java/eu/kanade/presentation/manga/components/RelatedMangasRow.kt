package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// PASTIKAN IMPORT BERIKUT BENAR SESUAI PROJECT KAMU:
import eu.kanade.presentation.browse.components.BrowseSourceCompactGridItem // Mengganti MangaItem yang private
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.manga.components.MangaCover // Pastikan import MangaCover.Book.ratio ada
import eu.kanade.tachiyomi.ui.manga.RelatedManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.presentation.core.components.material.padding

@Composable
fun RelatedMangasRow(
    relatedMangas: List<RelatedManga>?, // Pastikan bertipe List<RelatedManga>?
    getMangaState: @Composable (Manga) -> State<Manga>,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    when {
        relatedMangas == null -> {
            GlobalSearchLoadingResultItem()
        }
        relatedMangas.isNotEmpty() -> {
            RelatedMangaCardRow(
                relatedMangas = relatedMangas,
                getManga = getMangaState,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
            )
        }
        else -> {
            // Jika EmptyResultItem() private, ganti dengan komponen penampung kosong lain atau Box()
            Box(modifier = Modifier.padding(MaterialTheme.padding.small)) 
        }
    }
}

@Composable
fun RelatedMangaCardRow(
    relatedMangas: List<RelatedManga>,
    getManga: @Composable (Manga) -> State<Manga>,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    // Memastikan mapping list dari tipe Success terambil dengan aman
    val mangas = relatedMangas.filterIsInstance<RelatedManga.Success>().flatMap { it.mangaList }
    val loading = relatedMangas.filterIsInstance<RelatedManga.Loading>().firstOrNull()

    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        items(mangas, key = { "related-row-${it.id}" }) { itemManga ->
            val manga by getManga(itemManga)
            // Menggunakan item grid bawaan browse karena MangaItem bersifat private
            BrowseSourceCompactGridItem(
                manga = manga,
                onClick = { onMangaClick(manga) },
                onLongClick = { onLongClick(manga) },
                isSelected = false,
            )
        }
        if (loading != null) {
            item {
                RelatedMangaLoadingItem()
            }
        }
    }
}
