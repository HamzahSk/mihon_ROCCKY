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
 * One editable argument row in the playground: an optional label/name plus the
 * value that gets forwarded to the invoked script function.
 */
data class TestArg(
    val label: String = "",
    val value: String = "",
)

/**
 * Test runner for installed scripts. The "Test Function" section can invoke any named
 * function inside the script (`main`, `search`, `detail`, ...) with a dynamic,
 * user-controlled list of args. Function names are detected automatically from the
 * script source, so no manual entry is required; the default argument state is empty
 * unless the user explicitly adds inputs.
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
        private val FUNCTION_NAME_REGEX = Regex("function\\s+([a-zA-Z_$][\\w$]*)\\s*\\(")
    }

    data class State(
        val scripts: List<Script> = emptyList(),
        val selectedId: String? = null,
        val testFunction: String = "",
        val testFunctionSuggestions: List<String> = emptyList(),
        val testArgs: List<TestArg> = emptyList(),
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
                        testArgs = if (selectionChanged) emptyList() else current.testArgs,
                    )
                }
            }
        }
    }

    /**
     * Scans a script's source code and returns the names of every function declared
     * with the classic `function name(...)` syntax.
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
            testArgs = emptyList(),
            log = "",
        )
    }

    fun onTestFunctionChange(value: String) = mutableState.update { it.copy(testFunction = value) }

    fun addArg() = mutableState.update { it.copy(testArgs = it.testArgs + TestArg()) }

    fun removeArg(index: Int) = mutableState.update {
        if (index < 0 || index >= it.testArgs.size) it
        else it.copy(testArgs = it.testArgs.filterIndexed { i, _ -> i != index })
    }

    fun updateArgLabel(index: Int, label: String) = mutableState.update {
        if (index < 0 || index >= it.testArgs.size) it
        else it.copy(testArgs = it.testArgs.mapIndexed { i, arg -> if (i == index) arg.copy(label = label) else arg })
    }

    fun updateArgValue(index: Int, value: String) = mutableState.update {
        if (index < 0 || index >= it.testArgs.size) it
        else it.copy(testArgs = it.testArgs.mapIndexed { i, arg -> if (i == index) arg.copy(value = value) else arg })
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
            .map { it.value.trim() }
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
