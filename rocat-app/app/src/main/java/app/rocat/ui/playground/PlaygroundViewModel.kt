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
 * Test runner for installed scripts. The "Test Function" section can invoke any named
 * function inside the script (`main`, `search`, `detail`, ...) with a dynamic,
 * user-controlled list of plain values. Functions are detected automatically from the
 * script source; only public (non-underscore-prefixed) functions are offered as
 * suggestions. The default argument state is a single empty value row, and the user
 * adds more rows with "+ Add Input" when the function needs more arguments.
 *
 * Every execution failure (engine error, validation, or a stray Throwable) is written
 * INTO the log area instead of being rendered as floating red text, and is caught so a
 * runtime error from JS never force-closes the app.
 */
class PlaygroundViewModel(
    private val getScripts: GetScripts = Injekt.get(),
    private val executeScript: ExecuteScript = Injekt.get(),
) : StateViewModel<PlaygroundViewModel.State>(State()) {

    companion object {
        private val FUNCTION_NAME_REGEX = Regex("function\\s+(?!_)([a-zA-Z$][\\w$]*)\\s*\\(")
        private const val DEFAULT_ARGS_COUNT = 1
    }

    data class State(
        val scripts: List<Script> = emptyList(),
        val selectedId: String? = null,
        val testFunction: String = "",
        val testFunctionSuggestions: List<String> = emptyList(),
        val testArgs: List<String> = List(DEFAULT_ARGS_COUNT) { "" },
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
                    val script = enabled.firstOrNull { it.id == selection }
                    val functions = script?.let { extractFunctionNames(it.source) }.orEmpty()
                    val selectionChanged = selection != current.selectedId
                    current.copy(
                        scripts = enabled,
                        selectedId = selection,
                        testFunctionSuggestions = functions,
                        testFunction = if (selectionChanged) (functions.firstOrNull() ?: "") else current.testFunction,
                        testArgs = if (selectionChanged) List(DEFAULT_ARGS_COUNT) { "" } else current.testArgs,
                    )
                }
            }
        }
    }

    /**
     * Scans a script's source code and returns the names of every PUBLIC function
     * declared with the classic `function name(...)` syntax. Private functions whose
     * name starts with an underscore (`_helper`) are excluded.
     */
    fun extractFunctionNames(scriptCode: String): List<String> =
        FUNCTION_NAME_REGEX.findAll(scriptCode).map { it.groupValues[1] }.toList()

    fun select(id: String) = mutableState.update { current ->
        val script = current.scripts.firstOrNull { it.id == id }
        val functions = script?.let { extractFunctionNames(it.source) }.orEmpty()
        current.copy(
            selectedId = id,
            testFunctionSuggestions = functions,
            testFunction = functions.firstOrNull() ?: "",
            testArgs = List(DEFAULT_ARGS_COUNT) { "" },
            log = "",
        )
    }

    fun onTestFunctionChange(value: String) = mutableState.update { it.copy(testFunction = value) }

    fun addArg() = mutableState.update { it.copy(testArgs = it.testArgs + "") }

    fun removeArg(index: Int) = mutableState.update {
        if (index < 0 || index >= it.testArgs.size) it
        else it.copy(testArgs = it.testArgs.filterIndexed { i, _ -> i != index })
    }

    fun updateArgValue(index: Int, value: String) = mutableState.update {
        if (index < 0 || index >= it.testArgs.size) it
        else it.copy(testArgs = it.testArgs.mapIndexed { i, arg -> if (i == index) value else arg })
    }

    /**
     * Invokes the currently selected function name inside the script, forwarding the
     * (non-blank) values of every dynamic argument row. Shows the JSON result in the log.
     */
    fun runFunction() {
        val current = state.value
        val script = current.selectedScript
        if (script == null) {
            mutableState.update { it.copy(log = "Select an enabled script first") }
            return
        }
        val functionName = current.testFunction.trim()
        if (functionName.isEmpty()) {
            mutableState.update { it.copy(log = "Enter a function name first") }
            return
        }
        val args = current.testArgs
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mutableState.update { it.copy(executing = true, log = "") }
                val res = executeScript.invoke(script, functionName, args)
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
