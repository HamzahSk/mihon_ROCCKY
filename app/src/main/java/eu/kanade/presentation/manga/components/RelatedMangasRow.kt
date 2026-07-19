package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.RelatedMangasLoadingItem
import eu.kanade.presentation.library.components.MangaCompactGridItem // Menggunakan komponen dari CommonMangaItem
import eu.kanade.tachiyomi.ui.manga.RelatedManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.presentation.core.components.material.padding

@Composable
fun RelatedMangasRow(
    relatedMangas: List<RelatedManga>?,
    getMangaState: @Composable (Manga) -> State<Manga>,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    when {
        relatedMangas == null -> {
            RelatedMangasLoadingItem()
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
    val mangas = relatedMangas.filterIsInstance<RelatedManga.Success>().flatMap { it.mangaList }
    val loading = relatedMangas.filterIsInstance<RelatedManga.Loading>().firstOrNull()

    LazyRow(
        contentPadding = PaddingValues(MaterialTheme.padding.small),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
    ) {
        items(mangas, key = { "related-row-${it.id}" }) { itemManga ->
            val manga by getManga(itemManga)
            
            // Mengatur lebar card item agar rapi di dalam baris horizontal
            Box(modifier = Modifier.width(108.dp)) {
                MangaCompactGridItem(
                    coverData = manga.asMangaCover(),
                    title = manga.title,
                    onClick = { onMangaClick(manga) },
                    onLongClick = { onMangaLongClick(manga) },
                    isSelected = false,
                )
            }
        }
        if (loading != null) {
            item {
                RelatedMangasLoadingItem()
            }
        }
    }
}
