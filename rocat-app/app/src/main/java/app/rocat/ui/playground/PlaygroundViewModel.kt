package app.rocat.ui.playground

import androidx.lifecycle.viewModelScope
import app.rocat.core.common.injekt.Injekt
import app.rocat.core.viewmodel.StateViewModel
import app.rocat.domain.script.ExecuteScript
import app.rocat.domain.script.GetScripts
import app.rocat.scripting.api.ScriptResult
import app.rocat.scripting.api.model.Script
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Test runner for installed scripts. Only enabled scripts are runnable; the chosen
 * script's `main(targetUrl)` is executed and its return value shown in the UI.
 *
 * Every execution failure (engine error, validation, or a stray Throwable) is written
 * INTO the Result / log area instead of being rendered as floating red text, and is
 * caught so a runtime error from JS never force-closes the app.
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
        val testParam: String = "",
        val executing: Boolean = false,
        val log: String = "",
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

    fun select(id: String) = mutableState.update {
        it.copy(selectedId = id, result = "", log = "")
    }

    fun onUrlChange(value: String) = mutableState.update { it.copy(targetUrl = value) }

    fun onTestParamChange(value: String) = mutableState.update { it.copy(testParam = value) }

    fun run() {
        val current = state.value
        val script = current.selectedScript
        if (script == null) {
            mutableState.update { it.copy(result = "Select an enabled script first") }
            return
        }
        if (current.targetUrl.isBlank()) {
            mutableState.update { it.copy(result = "Enter a target URL") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mutableState.update { it.copy(running = true, result = "") }
                val res = executeScript.await(script, listOf(current.targetUrl))
                withContext(Dispatchers.Main) {
                    when (res) {
                        is ScriptResult.Success -> mutableState.update { it.copy(running = false, result = res.value) }
                        is ScriptResult.Failure -> mutableState.update { it.copy(running = false, result = "Error: ${res.error}") }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    mutableState.update { it.copy(running = false, result = "Error: ${e.message ?: e.javaClass.simpleName}") }
                }
            }
        }
    }

    /** Calls `search(query)` in the selected script and shows its JSON result. */
    fun runSearch() = runFunction("search")

    /** Calls `detail(url)` in the selected script and shows its JSON result. */
    fun runDetail() = runFunction("detail")

    private fun runFunction(functionName: String) {
        val current = state.value
        val script = current.selectedScript
        if (script == null) {
            mutableState.update { it.copy(log = "Select an enabled script first") }
            return
        }
        if (current.testParam.isBlank()) {
            mutableState.update { it.copy(log = "Enter a parameter (query or URL) first") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mutableState.update { it.copy(executing = true, log = "") }
                val res = executeScript.invoke(script, functionName, listOf(current.testParam.trim()))
                withContext(Dispatchers.Main) {
                    when (res) {
                        is ScriptResult.Success -> mutableState.update { it.copy(executing = false, log = res.value) }
                        is ScriptResult.Failure -> mutableState.update {
                            it.copy(executing = false, log = "Error: ${res.error}")
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    mutableState.update { it.copy(executing = false, log = "Error: ${e.message ?: e.javaClass.simpleName}") }
                }
            }
        }
    }
}