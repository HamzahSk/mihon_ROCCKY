package app.rocat.ui.playground

import androidx.lifecycle.viewModelScope
import app.rocat.core.common.injekt.Injekt
import app.rocat.core.viewmodel.StateViewModel
import app.rocat.domain.script.ExecuteScript
import app.rocat.domain.script.GetScripts
import app.rocat.scripting.api.ScriptResult
import app.rocat.scripting.api.model.Script
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Test runner for installed scripts. Only enabled scripts are runnable; the chosen
 * script's `main(targetUrl)` is executed and its return value shown in the UI.
 */
class PlaygroundViewModel(
    private val getScripts: GetScripts = Injekt.get(),
    private val executeScript: ExecuteScript = Injekt.get(),
) : StateViewModel<PlaygroundViewModel.State>(State()) {

    data class State(
        val scripts: List<Script> = emptyList(),
        val selectedId: String? = null,
        val targetUrl: String = "https://example.com/",
        val running: Boolean = false,
        val result: String = "",
        val error: String? = null,
    ) {
        val selectedScript: Script? get() = scripts.firstOrNull { it.id == selectedId }
    }

    init {
        viewModelScope.launch {
            getScripts.subscribe().collect { list ->
                val enabled = list.filter { it.enabled }
                mutableState.update { current ->
                    val selection = if (enabled.any { it.id == current.selectedId }) {
                        current.selectedId
                    } else {
                        enabled.firstOrNull()?.id
                    }
                    current.copy(scripts = enabled, selectedId = selection)
                }
            }
        }
    }

    fun select(id: String) = mutableState.update { it.copy(selectedId = id, result = "", error = null) }

    fun onUrlChange(value: String) = mutableState.update { it.copy(targetUrl = value) }

    fun run() {
        val current = state.value
        val script = current.selectedScript
        if (script == null) {
            mutableState.update { it.copy(error = "Select an enabled script first") }
            return
        }
        if (current.targetUrl.isBlank()) {
            mutableState.update { it.copy(error = "Enter a target URL") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(running = true, result = "", error = null) }
            when (val res = executeScript.await(script, listOf(current.targetUrl))) {
                is ScriptResult.Success -> mutableState.update { it.copy(running = false, result = res.value) }
                is ScriptResult.Failure -> mutableState.update { it.copy(running = false, error = res.error) }
            }
        }
    }
}
