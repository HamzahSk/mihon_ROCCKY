package tachiyomi.data.searchhistory

import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList

class SearchHistoryRepositoryImpl(
    private val database: Database,
) {
    // Memakai subscribeToList() agar UI otomatis merespons perubahan data
    fun getSearchHistory(sourceId: Long): Flow<List<String>> {
        return database.searchHistoryQueries
            .getHistoryBySource(sourceId)
            .subscribeToList()
    }

    suspend fun insertSearchHistory(sourceId: Long, query: String) {
        try {
            database.searchHistoryQueries.insert(
                sourceId = sourceId,
                searchQuery = query,
                dateSearched = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            // Tangani error dengan logcat seperti di HistoryRepositoryImpl
        }
    }

    suspend fun deleteSearchHistory(sourceId: Long, query: String) {
        try {
            database.searchHistoryQueries.deleteQuery(sourceId, query)
        } catch (e: Exception) {
            // Tangani error
        }
    }
}
