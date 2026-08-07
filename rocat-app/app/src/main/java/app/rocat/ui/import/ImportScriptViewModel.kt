package app.rocat.ui.import

import androidx.lifecycle.viewModelScope
import app.rocat.core.common.injekt.Injekt
import app.rocat.core.viewmodel.StateViewModel
import app.rocat.data.script.ScriptSourceFetcher
import app.rocat.domain.script.ImportScript
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportScriptViewModel(
    private val importScript: ImportScript = Injekt.get(),
    private val scriptSourceFetcher: ScriptSourceFetcher = Injekt.get(),
) : StateViewModel<ImportScriptViewModel.State>(State()) {

    data class State(
        val url: String = "",
        val source: String = "",
        val busy: Boolean = false,
        val message: String? = null,
        val error: String? = null,
    )

    fun onUrlChange(value: String) = mutableState.update { it.copy(url = value) }
    fun onSourceChange(value: String) = mutableState.update { it.copy(source = value) }

    fun importFromUrl(onImported: (String) -> Unit) {
        val url = state.value.url.trim()
        if (url.isEmpty()) {
            mutableState.update { it.copy(error = "Enter a script URL first") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, error = null, message = null) }
            try {
                val fetched = scriptSourceFetcher.fetchSource(url)
                val script = importScript.await(fetched)
                mutableState.update {
                    it.copy(busy = false, url = "", message = "Imported \"${script.name}\" v${script.version}")
                }
                onImported(script.id)
            } catch (e: Exception) {
                mutableState.update { it.copy(busy = false, error = e.message ?: "Import failed") }
            }
        }
    }

    fun importFromSource(onImported: (String) -> Unit) {
        val source = state.value.source
        if (source.isBlank()) {
            mutableState.update { it.copy(error = "Paste script source first") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, error = null, message = null) }
            try {
                val script = importScript.await(source)
                mutableState.update {
                    it.copy(busy = false, source = "", message = "Imported \"${script.name}\" v${script.version}")
                }
                onImported(script.id)
            } catch (e: Exception) {
                mutableState.update { it.copy(busy = false, error = e.message ?: "Import failed") }
            }
        }
    }

    fun loadExample() = mutableState.update { it.copy(source = EXAMPLE_SCRIPT) }

    companion object {
        /** A small Rhino-compatible sample (no async/await) for quick verification. */
        val EXAMPLE_SCRIPT = """
            // ==UserScript==
            // @name        Example JSON Fetcher
            // @version     1.0.0
            // @description Fetches the target URL and returns its raw text body.
            // @author      RoCat
            // @match       *
            // ==/UserScript==

            function main(url) {
                var res = fetch(url);
                if (!res.ok) {
                    return "HTTP " + res.status + " | error: " + res.error;
                }
                return res.text();
            }
        """.trimIndent()
    }
}
