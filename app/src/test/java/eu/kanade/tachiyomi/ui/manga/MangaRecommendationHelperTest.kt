package eu.kanade.tachiyomi.ui.manga

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MangaRecommendationHelperTest {

    @Test
    fun `genre priority keeps most frequent first and stable tie order`() {
        val history = listOf(
            RecentHistoryManga(
                title = "Alpha",
                genres = listOf("Action", "Drama"),
            ),
            RecentHistoryManga(
                title = "Beta",
                genres = listOf("Action", "Comedy"),
            ),
            RecentHistoryManga(
                title = "Gamma",
                genres = listOf("Romance", "Comedy"),
            ),
            RecentHistoryManga(
                title = "Delta",
                genres = listOf("Action"),
            ),
        )

        assertEquals(
            listOf("Action", "Comedy", "Drama", "Romance"),
            MangaRecommendationHelper.buildGenrePriority(history),
        )
    }

    @Test
    fun `genre attempts relax one by one while keeping top genre`() {
        val attempts = MangaRecommendationHelper.buildGenreAttempts(
            listOf("Action", "Comedy", "Drama"),
        )

        assertEquals(
            listOf(
                listOf("Action", "Comedy", "Drama"),
                listOf("Action", "Comedy"),
                listOf("Action"),
            ),
            attempts,
        )
    }

    @Test
    fun `fallback queries include top genre and history title tokens`() {
        val queries = MangaRecommendationHelper.buildFallbackQueries(
            historyMangas = listOf(
                RecentHistoryManga(
                    title = "My Hero Academia",
                    genres = listOf("Action"),
                ),
                RecentHistoryManga(
                    title = "One Piece",
                    genres = listOf("Adventure"),
                ),
            ),
            orderedGenres = listOf("Action", "Adventure"),
        )

        assertTrue(queries.isNotEmpty())
        assertEquals("Action", queries.first())
        assertTrue("Hero" in queries)
        assertTrue("Academia" in queries)
        assertTrue("One Piece" in queries)
    }
}
