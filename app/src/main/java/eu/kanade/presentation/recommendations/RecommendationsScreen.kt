package eu.kanade.presentation.recommendations

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.library.components.MangaCompactGridItem
import eu.kanade.tachiyomi.ui.recommendations.RecommendationsViewModel
import eu.kanade.tachiyomi.ui.recommendations.SortMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.BaseSortItem
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.FastScrollLazyVerticalGrid
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import kotlin.time.Duration.Companion.seconds
import tachiyomi.domain.manga.model.MangaCover as MangaCoverModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    state: RecommendationsViewModel.State,
    onClickManga: (Manga) -> Unit,
    onToggleSource: (Long) -> Unit,
    onRefresh: () -> Unit,
    onSetSortMode: (SortMode) -> Unit,
    onToggleGenreFilter: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenManageSources: () -> Unit,
    onDismissDialog: () -> Unit,
    onResetFilters: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        topBar = {
            var showMenu by remember { mutableStateOf(false) }
            TopAppBar(
                title = { Text(stringResource(MR.strings.label_recommendations)) },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = stringResource(MR.strings.action_filter),
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(MR.strings.label_more),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(MR.strings.pref_manage_sources)) },
                            onClick = {
                                showMenu = false
                                onOpenManageSources()
                            },
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        PullRefresh(
            refreshing = isRefreshing,
            onRefresh = {
                onRefresh()
                scope.launch {
                    isRefreshing = true
                    delay(1.seconds)
                    isRefreshing = false
                }
            },
            enabled = true,
        ) {
            if (state.isLoading && state.recommendations.isEmpty()) {
                LoadingScreen(modifier = Modifier.padding(contentPadding))
            } else if (state.recommendations.isEmpty()) {
                EmptyScreen(
                    modifier = Modifier.padding(contentPadding),
                    stringRes = MR.strings.information_empty_recommendations,
                )
            } else {
                FastScrollLazyVerticalGrid(
                    columns = GridCells.Adaptive(128.dp),
                    contentPadding = contentPadding + PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        RecommendationsCarousel(
                            mangas = state.recommendations,
                            onMangaClick = onClickManga,
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(MR.strings.label_recommendations),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.padding.medium,
                                vertical = 4.dp,
                            ),
                        )
                    }

                    items(state.recommendations.size) { index ->
                        val manga = state.recommendations[index]
                        MangaCompactGridItem(
                            title = manga.title,
                            coverData = MangaCoverModel(
                                mangaId = manga.id,
                                sourceId = manga.source,
                                isMangaFavorite = manga.favorite,
                                url = manga.thumbnailUrl,
                                lastModified = manga.coverLastModified,
                            ),
                            onClick = { onClickManga(manga) },
                            onLongClick = {},
                        )
                    }
                }
            }
        }
    }

    when (state.dialog) {
        is RecommendationsViewModel.State.Dialog.Settings -> {
            RecommendationsSettingsDialog(
                onDismissRequest = onDismissDialog,
                sortMode = state.sortMode,
                onSetSortMode = onSetSortMode,
                selectedGenres = state.selectedGenres,
                availableGenres = state.availableGenres,
                onToggleGenreFilter = onToggleGenreFilter,
                onResetFilters = onResetFilters,
            )
        }
        is RecommendationsViewModel.State.Dialog.ManageSources -> {
            ManageSourcesDialog(
                availableSources = state.availableSources,
                onToggleSource = onToggleSource,
                onDismissRequest = onDismissDialog,
            )
        }
        null -> {}
    }
}

@Composable
fun RecommendationsSettingsDialog(
    onDismissRequest: () -> Unit,
    sortMode: SortMode,
    onSetSortMode: (SortMode) -> Unit,
    selectedGenres: Set<String>,
    availableGenres: List<String>,
    onToggleGenreFilter: (String) -> Unit,
    onResetFilters: () -> Unit,
) {
    val tabTitles = buildList {
        add(stringResource(MR.strings.action_filter))
        add(stringResource(MR.strings.action_sort))
    }

    TabbedDialog(
        onDismissRequest = onDismissRequest,
        tabTitles = tabTitles,
    ) { page ->
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            when (page) {
                0 -> {
                    HeadingItem(stringResource(MR.strings.action_filter))
                    if (availableGenres.isEmpty()) {
                        Text(
                            text = stringResource(MR.strings.information_empty_recommendations),
                            modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        availableGenres.forEach { genre ->
                            val isSelected = genre in selectedGenres
                            CheckboxItem(
                                label = genre.replaceFirstChar { it.uppercase() },
                                checked = isSelected,
                                onClick = { onToggleGenreFilter(genre) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onResetFilters) {
                        Text(stringResource(MR.strings.action_reset))
                    }
                }
                1 -> {
                    HeadingItem(stringResource(MR.strings.action_sort))

                    val sortOptions = listOf(
                        SortMode.DEFAULT to MR.strings.label_default,
                        SortMode.LATEST_UPDATE to MR.strings.action_sort_last_manga_update,
                        SortMode.RANDOM to MR.strings.action_sort_random,
                        SortMode.CHAPTER_COUNT to MR.strings.action_sort_total,
                    )

                    sortOptions.forEach { (mode, labelRes) ->
                        val selected = sortMode == mode
                        BaseSortItem(
                            label = stringResource(labelRes),
                            icon = if (selected) Icons.Default.Check else null,
                            onClick = {
                                onSetSortMode(mode)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ManageSourcesDialog(
    availableSources: List<RecommendationsViewModel.SourceItem>,
    onToggleSource: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AdaptiveSheet(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = TabbedDialogPaddings.Vertical)
                .verticalScroll(rememberScrollState()),
        ) {
            HeadingItem(stringResource(MR.strings.pref_manage_sources))
            if (availableSources.isEmpty()) {
                Text(
                    text = stringResource(MR.strings.information_empty_recommendations),
                    modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                availableSources.forEach { source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSource(source.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = source.enabled,
                            onCheckedChange = { onToggleSource(source.id) },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = source.name,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationsCarousel(
    mangas: List<Manga>,
    onMangaClick: (Manga) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemCount = minOf(mangas.size, 5)
    if (itemCount > 0) {
        val pagerState = rememberPagerState(pageCount = { itemCount })
        val activeManga = mangas.getOrNull(pagerState.currentPage)
        val surfaceColor = MaterialTheme.colorScheme.surface

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(320.dp),
        ) {
            if (activeManga != null) {
                AsyncImage(
                    model = activeManga,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(20.dp)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Black.copy(alpha = 0.7f),
                                        surfaceColor,
                                    ),
                                ),
                            )
                        },
                    contentScale = ContentScale.Crop,
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 12.dp, bottom = 32.dp),
                contentPadding = PaddingValues(horizontal = 48.dp),
                pageSpacing = 16.dp,
            ) { page ->
                val manga = mangas[page]
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onMangaClick(manga) },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = manga,
                            contentDescription = "Cover for ${manga.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f),
                                        ),
                                    ),
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = manga.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            CarouselDotsIndicator(
                itemCount = itemCount,
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
fun CarouselDotsIndicator(
    itemCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(itemCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 18.dp else 8.dp,
                label = "dot_width",
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(4.dp),
                    ),
            )
        }
    }
}
