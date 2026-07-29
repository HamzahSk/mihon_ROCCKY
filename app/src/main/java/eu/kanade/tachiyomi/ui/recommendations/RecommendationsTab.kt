package eu.kanade.tachiyomi.ui.recommendations

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cash.sqldelight.async.coroutines.awaitAsList
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.recommendations.RecommendationsScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import kotlin.random.Random

data object RecommendationsTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_recommendations_enter)
            return TabOptions(
                index = 2u,
                title = stringResource(MR.strings.label_recommendations),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {}

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel<RecommendationsViewModel>()
        val state by viewModel.state.collectAsState()

        RecommendationsScreen(
            state = state,
            onClickManga = { manga ->
                viewModel.onMangaClick(manga) { mangaId ->
                    navigator.push(MangaScreen(mangaId))
                }
            },
            onToggleSource = viewModel::toggleSource,
            onRefresh = viewModel::refreshRecommendations,
            onSetSortMode = viewModel::setSortMode,
            onToggleGenreFilter = viewModel::toggleGenreFilter,
            onOpenSettings = viewModel::openSettings,
            onDismissDialog = viewModel::dismissDialog,
            onResetFilters = viewModel::resetFilters,
        )
    }
}

enum class SortMode {
    DEFAULT,
    LATEST_UPDATE,
    RANDOM,
    CHAPTER_COUNT,
}

class RecommendationsViewModel(
    private val sourceManager: SourceManager = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val database: Database = Injekt.get(),
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var rawRecommendations: List<Manga> = emptyList()

    private val recommendedSourceIds: Set<String>
        get() = sourcePreferences.recommendedSources.get()

    init {
        loadSources()
        refreshRecommendations()
    }

    private fun loadSources() {
        val sources = sourceManager.getAll()
            .filterIsInstance<CatalogueSource>()
            .filter { it.id != 0L }
        _state.update {
            it.copy(
                availableSources = sources.map { s ->
                    SourceItem(
                        id = s.id,
                        name = s.name,
                        lang = s.lang,
                        enabled = s.id.toString() in recommendedSourceIds,
                    )
                },
            )
        }
    }

    fun toggleSource(sourceId: Long) {
        val current = sourcePreferences.recommendedSources.get().toMutableSet()
        val key = sourceId.toString()
        if (key in current) {
            current.remove(key)
        } else {
            current.add(key)
        }
        sourcePreferences.recommendedSources.set(current)
        loadSources()
        refreshRecommendations()
    }

    fun toggleGenreFilter(genre: String) {
        _state.update { current ->
            val updated = current.selectedGenres.toMutableSet()
            if (genre in updated) updated.remove(genre) else updated.add(genre)
            current.copy(
                selectedGenres = updated,
                recommendations = applyGenreFilter(rawRecommendations, updated, current.sortMode),
            )
        }
    }

    fun resetFilters() {
        _state.update { current ->
            current.copy(
                selectedGenres = emptySet(),
                recommendations = applyGenreFilter(rawRecommendations, emptySet(), current.sortMode),
            )
        }
    }

    fun refreshRecommendations() {
        viewModelScope.launchIO {
            _state.update { it.copy(isRefreshing = true) }
            val selectedIds = recommendedSourceIds.mapNotNull { it.toLongOrNull() }
            if (selectedIds.isEmpty()) {
                rawRecommendations = emptyList()
                _state.update { it.copy(isRefreshing = false, isLoading = false, recommendations = emptyList()) }
                return@launchIO
            }

            val allManga = mutableListOf<Manga>()
            for (sourceId in selectedIds) {
                val source = sourceManager.get(sourceId) as? CatalogueSource ?: continue
                try {
                    val page = source.getPopularManga(1)
                    val mangas = page.mangas.map { it.toDomainManga(source.id) }
                    allManga.addAll(mangas.take(8))
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Failed to fetch recommendations from source $sourceId" }
                }
                if (allManga.size >= 45) break
            }

            rawRecommendations = allManga

            val topGenres = getTopGenresFromHistory()
            _state.update { current ->
                current.copy(
                    availableGenres = topGenres,
                    isLoading = false,
                    isRefreshing = false,
                    recommendations = applyGenreFilter(allManga, current.selectedGenres, current.sortMode),
                )
            }
        }
    }

    private suspend fun getTopGenresFromHistory(): List<String> {
        return try {
            val selectedIds = recommendedSourceIds.mapNotNull { it.toLongOrNull() }
            if (selectedIds.isEmpty()) return emptyList()

            val allGenres = mutableListOf<String>()
            for (sourceId in selectedIds) {
                try {
                    val historyGenres = database.historyQueries.getHistoryBySource(
                        sourceId,
                        5L,
                    ) { _id: Long, chapter_id: Long, last_read: Date?, time_read: Long, genres: List<String>? ->
                        genres ?: emptyList()
                    }.awaitAsList()
                    allGenres.addAll(historyGenres.flatten())
                } catch (_: Exception) {}
            }

            val flatGenres = allGenres
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { it.lowercase() }

            if (flatGenres.isEmpty()) return emptyList()

            flatGenres.groupingBy { it }.eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key }
                .take(10)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to read history genres for recommendations" }
            emptyList()
        }
    }

    private fun applyGenreFilter(
        list: List<Manga>,
        selectedGenres: Set<String>,
        sortMode: SortMode,
    ): List<Manga> {
        val activeGenres = if (selectedGenres.isNotEmpty()) {
            selectedGenres
        } else {
            _state.value.availableGenres.take(5).toSet()
        }

        val scored = list.map { manga ->
            val mangaGenres = manga.genre?.map { it.trim().lowercase() } ?: emptyList()
            val matchCount = mangaGenres.count { it in activeGenres }
            manga to matchCount
        }

        val filtered = scored.filter { it.second > 0 }

        val sorted = when (sortMode) {
            SortMode.DEFAULT -> filtered.sortedByDescending { it.second }
            SortMode.LATEST_UPDATE -> filtered.sortedByDescending { it.first.lastUpdate }
            SortMode.RANDOM -> filtered.shuffled(Random)
            SortMode.CHAPTER_COUNT -> filtered.sortedByDescending { it.second }
        }

        return sorted.map { it.first }
    }

    fun setSortMode(sortMode: SortMode) {
        _state.update {
            it.copy(
                sortMode = sortMode,
                recommendations = applyGenreFilter(rawRecommendations, it.selectedGenres, sortMode),
            )
        }
    }

    fun openSettings() {
        _state.update { it.copy(dialog = State.Dialog.Settings) }
    }

    fun dismissDialog() {
        _state.update { it.copy(dialog = null) }
    }

    fun onMangaClick(manga: Manga, onClick: (Long) -> Unit) {
        viewModelScope.launchIO {
            val localManga = networkToLocalManga(manga)
            withContext(Dispatchers.Main) {
                onClick(localManga.id)
            }
        }
    }

    data class State(
        val isLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val recommendations: List<Manga> = emptyList(),
        val availableSources: List<SourceItem> = emptyList(),
        val sortMode: SortMode = SortMode.DEFAULT,
        val dialog: Dialog? = null,
        val selectedGenres: Set<String> = emptySet(),
        val availableGenres: List<String> = emptyList(),
    ) {
        sealed interface Dialog {
            data object Settings : Dialog
        }
    }

    data class SourceItem(
        val id: Long,
        val name: String,
        val lang: String,
        val enabled: Boolean,
    )
}
