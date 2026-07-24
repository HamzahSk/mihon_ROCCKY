package eu.kanade.tachiyomi.ui.manga

internal data class RecentHistoryManga(
    val title: String,
    val genres: List<String>,
)

internal object MangaRecommendationHelper {

    private data class GenreBucket(
        val count: Int,
        val display: String,
        val firstSeenIndex: Int,
    )

    fun buildGenrePriority(historyMangas: List<RecentHistoryManga>): List<String> {
        val buckets = linkedMapOf<String, GenreBucket>()

        historyMangas.forEachIndexed { historyIndex, manga ->
            manga.genres.asSequence()
                .mapNotNull { normalizeGenre(it) }
                .distinct()
                .forEach { genre ->
                    val key = genre.lowercase()
                    val current = buckets[key]
                    buckets[key] = if (current == null) {
                        GenreBucket(
                            count = 1,
                            display = genre,
                            firstSeenIndex = historyIndex,
                        )
                    } else {
                        current.copy(
                            count = current.count + 1,
                            firstSeenIndex = minOf(current.firstSeenIndex, historyIndex),
                        )
                    }
                }
        }

        return buckets.values
            .sortedWith(
                compareByDescending<GenreBucket> { it.count }
                    .thenBy { it.firstSeenIndex },
            )
            .map { it.display }
    }

    fun buildGenreAttempts(orderedGenres: List<String>): List<List<String>> {
        if (orderedGenres.isEmpty()) return emptyList()

        return orderedGenres.indices.map { dropCount ->
            orderedGenres.take(orderedGenres.size - dropCount)
        }
    }

    fun buildFallbackQueries(
        historyMangas: List<RecentHistoryManga>,
        orderedGenres: List<String>,
    ): List<String> {
        val queries = linkedSetOf<String>()

        fun addCandidate(candidate: String?) {
            val value = candidate?.trim().orEmpty()
            if (value.isNotEmpty()) {
                queries += value
            }
        }

        addCandidate(orderedGenres.firstOrNull())

        historyMangas.asSequence()
            .flatMap { tokenizeTitle(it.title).asSequence() }
            .forEach(::addCandidate)

        historyMangas.asSequence()
            .map { it.title }
            .forEach(::addCandidate)

        return queries.toList()
    }

    private fun tokenizeTitle(title: String): List<String> {
        return title.split(Regex("[^\\p{L}\\p{Nd}]+"))
            .asSequence()
            .map { it.trim() }
            .filter { it.length >= 2 && it.any(Char::isLetter) }
            .distinct()
            .toList()
    }

    private fun normalizeGenre(value: String?): String? {
        return value?.trim()?.takeIf { it.isNotEmpty() }
    }
}
