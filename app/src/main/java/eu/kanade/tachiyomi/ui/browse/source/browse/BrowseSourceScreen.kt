package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.LazyPagingItems

import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.presentation.browse.BrowseSourceContent
import eu.kanade.presentation.browse.MissingSourceScreen
import eu.kanade.presentation.browse.components.BrowseSourceToolbar
import eu.kanade.presentation.browse.components.RemoveMangaDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.manga.DuplicateMangaDialog
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel.Listing
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow

import mihon.feature.migration.dialog.MigrateMangaDialog
import mihon.presentation.core.util.collectAsLazyPagingItems

import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.LocalSource

data class BrowseSourceScreen(
    val sourceId: Long,
    private val listingQuery: String?,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val viewModel = viewModel<BrowseSourceViewModel>(
            factory = BrowseSourceViewModel.Factory,
            extras = CreationExtras {
                set(BrowseSourceViewModel.SOURCE_ID_KEY, sourceId)
                set(BrowseSourceViewModel.LISTING_QUERY_KEY, listingQuery)
            },
        )
        val state by viewModel.state.collectAsState()
        
        val searchHistory by viewModel.searchHistory.collectAsState()
        
        val mangaList = viewModel.mangaPagerFlowFlow.collectAsLazyPagingItems()

        val navigator = LocalNavigator.currentOrThrow
        val navigateUp: () -> Unit = {
            when {
                !state.isUserQuery && state.toolbarQuery != null -> viewModel.setToolbarQuery(null)
                else -> navigator.pop()
            }
        }

        if (viewModel.source is StubSource) {
            MissingSourceScreen(
                source = viewModel.source,
                navigateUp = navigateUp,
            )
            return
        }

        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val snackbarHostState = remember { SnackbarHostState() }

        var isCarouselVisible by remember { mutableStateOf(true) }
        
        // --- LOGIC BUAT NGE-TRACK GAMBAR MANGA YG LAGI AKTIF ---
        val recommendationsCount = minOf(state.recommendations.size, 5)
        val listCount = minOf(mangaList.itemCount, 5).coerceAtLeast(0)
        
        val recPagerState = rememberPagerState(pageCount = { recommendationsCount })
        val listPagerState = rememberPagerState(pageCount = { listCount })

        val recManga = if (state.recommendations.isNotEmpty() && recPagerState.currentPage < state.recommendations.size) {
            state.recommendations[recPagerState.currentPage]
        } else null
        
        val listManga = if (state.recommendations.isEmpty() && mangaList.itemCount > 0 && listPagerState.currentPage < mangaList.itemCount) {
            mangaList.peek(listPagerState.currentPage)?.collectAsState()?.value
        } else null

        val currentManga = recManga ?: listManga
        // --------------------------------------------------------

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    if (delta < -10f) {
                        isCarouselVisible = false
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (available.y > 0f) {
                        isCarouselVisible = true
                    }
                    return Offset.Zero
                }
            }
        }

        val onHelpClick = { uriHandler.openUri(LocalSource.HELP_URL) }
        val onWebViewClick = f@{
            val source = viewModel.source as? HttpSource ?: return@f
            navigator.push(
                WebViewScreen(
                    url = source.getHomeUrl(),
                    initialTitle = source.name,
                    sourceId = source.id,
                ),
            )
        }

        LaunchedEffect(viewModel.source) {
            if (viewModel.source !is StubSource) {
                viewModel.applyRecommendationsFromHistory()
            }
            assistUrl = (viewModel.source as? HttpSource)?.getHomeUrl()
        }

        Scaffold(
            modifier = Modifier.nestedScroll(nestedScrollConnection),
            topBar = {
                // BOX BUAT BIKIN LAYER BACKGROUND + FOREGROUND
                Box(modifier = Modifier.fillMaxWidth()) {
                    
                    // LAYER 1: Background Image Hero (Thumbnails)
                    AnimatedVisibility(
                        visible = isCarouselVisible && currentManga != null,
                        enter = fadeIn(animationSpec = tween(500)),
                        exit = fadeOut(animationSpec = tween(500)),
                        modifier = Modifier.matchParentSize()
                    ) {
                        if (currentManga != null) {
                            AsyncImage(
                                model = currentManga,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.8f) // Diterangin dikit biar gak terlalu gelap
                            )
                            // Gradient Scrim biar text di Toolbar tetep jelas dibaca!
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), // Atas gelap
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), // Tengah transparan
                                                MaterialTheme.colorScheme.surface                      // Bawah nge-blend
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // LAYER 2: Konten Asli (Toolbar, Carousel, Chips)
                    Column(
                        modifier = Modifier
                            .background(if (isCarouselVisible) Color.Transparent else MaterialTheme.colorScheme.surface)
                            .pointerInput(Unit) {},
                    ) {
                        // Hack biar BrowseSourceToolbar jadi transparan kalau lg full atas
                        val currentColorScheme = MaterialTheme.colorScheme
                        val toolbarColorScheme = if (isCarouselVisible) {
                            currentColorScheme.copy(
                                surface = Color.Transparent,
                                background = Color.Transparent,
                                surfaceVariant = Color.Transparent
                            )
                        } else currentColorScheme

                        MaterialTheme(colorScheme = toolbarColorScheme) {
                            BrowseSourceToolbar(
                                searchQuery = state.toolbarQuery,
                                onSearchQueryChange = viewModel::setToolbarQuery,
                                source = viewModel.source,
                                displayMode = viewModel.displayMode,
                                onDisplayModeChange = { viewModel.displayMode = it },
                                navigateUp = navigateUp,
                                onWebViewClick = onWebViewClick,
                                onHelpClick = onHelpClick,
                                onSettingsClick = { navigator.push(SourcePreferencesScreen(sourceId)) },
                                onSearch = viewModel::search,
                                recentSearches = searchHistory,
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = isCarouselVisible,
                            enter = expandVertically(
                                animationSpec = tween(durationMillis = 500)
                            ),
                            exit = shrinkVertically(
                                animationSpec = tween(durationMillis = 500)
                            )
                        ) {
                            if (state.recommendations.isNotEmpty()) {
                                MangaCarouselRecommendations(
                                    mangas = state.recommendations,
                                    pagerState = recPagerState,
                                    onMangaClick = { navigator.push(MangaScreen(it.id, true)) }
                                )
                            } else {
                                MangaCarousel(
                                    mangaList = mangaList,
                                    pagerState = listPagerState,
                                    onMangaClick = { navigator.push(MangaScreen(it.id, true)) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = MaterialTheme.padding.small, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        ) {
                            FilterChip(
                                selected = state.listing == Listing.Popular,
                                onClick = {
                                    viewModel.resetFilters()
                                    viewModel.setListing(Listing.Popular)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                },
                                label = { Text(text = stringResource(MR.strings.popular)) },
                            )
                            if (viewModel.source.supportsLatest) {
                                FilterChip(
                                    selected = state.listing == Listing.Latest,
                                    onClick = {
                                        viewModel.resetFilters()
                                        viewModel.setListing(Listing.Latest)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.NewReleases,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                                        )
                                    },
                                    label = { Text(text = stringResource(MR.strings.latest)) },
                                )
                            }
                            if (state.filters.isNotEmpty()) {
                                FilterChip(
                                    selected = state.listing is Listing.Search,
                                    onClick = viewModel::openFilterSheet,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.FilterList,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                                        )
                                    },
                                    label = { Text(text = stringResource(MR.strings.action_filter)) },
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { paddingValues ->
            BrowseSourceContent(
                source = viewModel.source,
                mangaList = mangaList,
                columns = viewModel.getColumnsPreference(LocalConfiguration.current.orientation),
                displayMode = viewModel.displayMode,
                snackbarHostState = snackbarHostState,
                contentPadding = paddingValues,
                onWebViewClick = onWebViewClick,
                onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                onLocalSourceHelpClick = onHelpClick,
                onMangaClick = { navigator.push((MangaScreen(it.id, true))) },
                onMangaLongClick = { manga ->
                    scope.launchIO {
                        val duplicates = viewModel.getDuplicateLibraryManga(manga)
                        when {
                            manga.favorite -> viewModel.setDialog(BrowseSourceViewModel.Dialog.RemoveManga(manga))
                            duplicates.isNotEmpty() -> viewModel.setDialog(
                                BrowseSourceViewModel.Dialog.AddDuplicateManga(manga, duplicates),
                            )
                            else -> viewModel.addFavorite(manga)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
            )
        }

        val onDismissRequest = { viewModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is BrowseSourceViewModel.Dialog.Filter -> {
                SourceFilterDialog(
                    onDismissRequest = onDismissRequest,
                    filters = state.filters,
                    onReset = viewModel::resetFilters,
                    onFilter = { viewModel.search(filters = state.filters) },
                    onUpdate = viewModel::setFilters,
                )
            }
            is BrowseSourceViewModel.Dialog.AddDuplicateManga -> {
                DuplicateMangaDialog(
                    duplicates = dialog.duplicates,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { viewModel.addFavorite(dialog.manga) },
                    onOpenManga = { navigator.push(MangaScreen(it.id)) },
                    onMigrate = { viewModel.setDialog(BrowseSourceViewModel.Dialog.Migrate(dialog.manga, it)) },
                )
            }
            is BrowseSourceViewModel.Dialog.Migrate -> {
                MigrateMangaDialog(
                    current = dialog.current,
                    target = dialog.target,
                    onClickTitle = { navigator.push(MangaScreen(dialog.current.id)) },
                    onDismissRequest = onDismissRequest,
                )
            }
            is BrowseSourceViewModel.Dialog.RemoveManga -> {
                RemoveMangaDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = { viewModel.changeMangaFavorite(dialog.manga) },
                    mangaToRemove = dialog.manga,
                )
            }
            is BrowseSourceViewModel.Dialog.ChangeMangaCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen()) },
                    onConfirm = { include, _ ->
                        viewModel.changeMangaFavorite(dialog.manga)
                        viewModel.moveMangaToCategories(dialog.manga, include)
                    },
                )
            }
            else -> {}
        }

        LaunchedEffect(Unit) {
            queryEvent.receiveAsFlow()
                .collectLatest {
                    when (it) {
                        is SearchType.Genre -> viewModel.searchGenre(it.txt)
                        is SearchType.Text -> viewModel.search(it.txt)
                    }
                }
        }
    }

    suspend fun search(query: String) = queryEvent.send(SearchType.Text(query))
    suspend fun searchGenre(name: String) = queryEvent.send(SearchType.Genre(name))

    companion object {
        private val queryEvent = Channel<SearchType>()
    }

    sealed class SearchType(val txt: String) {
        class Text(txt: String) : SearchType(txt)
        class Genre(txt: String) : SearchType(txt)
    }
}

@Composable
fun MangaCarousel(
    mangaList: LazyPagingItems<StateFlow<Manga>>,
    pagerState: PagerState,
    onMangaClick: (Manga) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemCount = minOf(mangaList.itemCount, 5)

    if (itemCount > 0) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp) // Lebih tinggi biar kesannya wah
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp), // Kurangin padding biar lebih lebar
            pageSpacing = 16.dp
        ) { page ->
            val mangaFlow = mangaList[page]
            val manga = mangaFlow?.collectAsState()?.value

            if (manga != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMangaClick(manga) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = manga,
                            contentDescription = "Cover for ${manga.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = manga.title,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MangaCarouselRecommendations(
    mangas: List<Manga>,
    pagerState: PagerState,
    onMangaClick: (Manga) -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemCount = minOf(mangas.size, 5)
    if (itemCount > 0) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier
                .fillMaxWidth()
                .height(260.dp) // Lebih tinggi 
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp), // Lebih lebar!
            pageSpacing = 16.dp
        ) { page ->
            val manga = mangas[page]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMangaClick(manga) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = manga,
                        contentDescription = "Cover for ${manga.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = manga.title,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
