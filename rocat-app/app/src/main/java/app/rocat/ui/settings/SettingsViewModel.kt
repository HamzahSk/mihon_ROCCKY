package app.rocat.ui.settings

import android.net.Uri
import androidx.lifecycle.viewModelScope
import app.rocat.core.common.injekt.Injekt
import app.rocat.core.viewmodel.StateViewModel
import app.rocat.data.db.CookieDao
import app.rocat.data.db.HistoryDao
import app.rocat.i18n.AppLanguage
import app.rocat.i18n.I18nProvider
import app.rocat.i18n.StringKey
import app.rocat.settings.SettingsRepository
import app.rocat.storage.StorageManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the Settings screen. Coordinates the custom i18n provider, the SAF storage
 * manager and the Room DAOs for the three destructive actions (cache/cookies/history).
 * Every action updates [State.message] so the UI can surface a transient confirmation.
 */
class SettingsViewModel(
    private val settings: SettingsRepository = Injekt.get(),
    private val storageManager: StorageManager = Injekt.get(),
    private val i18nProvider: I18nProvider = Injekt.get(),
    private val cookieDao: CookieDao = Injekt.get(),
    private val historyDao: HistoryDao = Injekt.get(),
) : StateViewModel<SettingsViewModel.State>(State()) {

    data class State(
        val language: AppLanguage = AppLanguage.ENGLISH,
        val storageConfigured: Boolean = false,
        val storageName: String = "",
        val busy: Boolean = false,
        val message: StringKey? = null,
    )

    val settingsState: StateFlow<State> = i18nProvider.language
        .map { language ->
            State(
                language = language,
                storageConfigured = storageManager.isConfigured,
                storageName = storageManager.mainDirectoryName(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    fun setLanguage(language: AppLanguage) {
        i18nProvider.setLanguage(language)
    }

    /** Callback of the `OpenDocumentTree` launcher in the Settings screen. */
    fun onStoragePicked(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val persisted = storageManager.takePersistablePermission(uri)
            mutableState.value = mutableState.value.copy(
                storageConfigured = storageManager.isConfigured,
                storageName = storageManager.mainDirectoryName(),
                message = if (persisted) StringKey.storageChanged else StringKey.storagePermissionDenied,
            )
        }
    }

    fun clearCache() = mutateOnResult(StringKey.cacheCleared, StringKey.failure) {
        storageManager.clearCache()
    }

    fun deleteCookies() = mutateOnResult(StringKey.cookiesCleared, StringKey.failure) {
        cookieDao.deleteAll()
    }

    fun deleteHistory() = mutateOnResult(StringKey.historyCleared, StringKey.failure) {
        historyDao.deleteAll()
    }

    fun consumeMessage() {
        mutableState.value = mutableState.value.copy(message = null)
    }

    private fun mutateOnResult(success: StringKey, failure: StringKey, block: suspend () -> Unit) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(busy = true)
            val message = try {
                block()
                success
            } catch (e: Exception) {
                failure
            }
            mutableState.value = mutableState.value.copy(busy = false, message = message)
        }
    }
}
