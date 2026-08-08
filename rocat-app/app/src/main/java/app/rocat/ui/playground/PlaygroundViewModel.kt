package app.rocat.ui.playground

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewModelScope
import app.rocat.core.common.injekt.Injekt
import app.rocat.core.viewmodel.StateViewModel
import app.rocat.data.script.ScriptManager
import app.rocat.domain.script.ExecuteScript
import app.rocat.domain.script.GetScripts
import app.rocat.scripting.api.ScriptResult
import app.rocat.scripting.api.ScriptUiBridge
import app.rocat.scripting.api.model.Script
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Script-driven playground, mihon-styleTab. Instead of a fixed input/function picker,
 * the selected script builds its own UI by calling `RoCatUI.*` through a native bridge:
 *
 *  - `RoCatUI.addInput(id, hint)` / `addButton(label, fn)` / `thumbnailPreview(url)` /
 *    `videoPreview(url)` / `log(text)` / `clear()` manipulate [uiComponents].
 *  - On first load (and on every script switch / "Build UI") the script's `buildUI()`
 *    function is invoked to render the initial state.
 *  - Pressing a script button gathers every non-blank input into a `Map<id, value>` and
 *    invokes the button's JS function with that object as a single argument.
 *
 * All bridge callbacks are marshalled to the main thread and guarded by a session token
 * so a stale `buildUI()` from an older script can never wipe a newer render.
 */
class PlaygroundViewModel(
    private val getScripts: GetScripts = Injekt.get(),
    private val scriptManager: ScriptManager = Injekt.get(),
) : StateViewModel<PlaygroundViewModel.State>(State()) {

    data class State(
        val scripts: List<Script> = emptyList(),
        val selectedId: String? = null,
        val executing: Boolean = false,
        val log: String = "",
    ) {
        val selectedScript: Script? get() = scripts.firstOrNull { it.id == selectedId }
    }

    /** The ordered, script-driven list of components rendered by the playground. */
    val uiComponents: SnapshotStateList<ScriptUIComponent> = mutableStateListOf()

    /**
     * Monotonic session id. Incremented whenever a fresh `buildUI()` render starts so
     * queued bridge updates from an earlier render are discarded on the main thread.
     */
    @Volatile
    private var uiSession: Long = 0

    private val uiBridge = object : ScriptUiBridge {
        override fun addInput(id: String, hint: String) = postUi(uiSession) { addOrReplaceInput(id, hint) }
        override fun addButton(label: String, functionName: String) = postUi(uiSession) {
            uiComponents.add(ScriptUIComponent.Button(label, functionName))
        }
        override fun thumbnailPreview(url: String) = postUi(uiSession) {
            uiComponents.add(ScriptUIComponent.Thumbnail(url))
        }
        override fun videoPreview(url: String) = postUi(uiSession) {
            uiComponents.add(ScriptUIComponent.Video(url))
        }
        override fun clear() = postUi(uiSession) { uiComponents.clear() }
        override fun log(text: String) = postUi(uiSession) {
            uiComponents.add(ScriptUIComponent.LogText(text))
        }
    }

    /**
     * The engine/environment pair used for every script-driven invocation. It shares the
     * app's script client (fetch/stealth/cloudflare stack) and exposes [uiBridge] to the
     * script as the global `RoCatUI`.
     */
    private val uiExecuteScript = ExecuteScript(
        engine = scriptManager.engine,
        environment = scriptManager.createEnvironment(uiBridge),
    )

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
                state.value.selectedScript?.let { loadUi(it) }
            }
        }
    }

    fun select(id: String) {
        val script = mutableState.value.scripts.firstOrNull { it.id == id }
        mutableState.update { it.copy(selectedId = id, log = "") }
        script?.let { loadUi(it) }
    }

    /** Re-runs the selected script's `buildUI()` to re-render the component list. */
    fun rebuildUi() {
        state.value.selectedScript?.let { loadUi(it) }
    }

    /**
     * Starts a fresh UI render: clears the previous components and invokes the script's
     * `buildUI()` function (when present) which repopulates the list via `RoCatUI.*`.
     */
    private fun loadUi(script: Script) {
        uiSession++
        val session = uiSession
        postUi(session) { uiComponents.clear() }

        viewModelScope.launch {
            val result = try {
                uiExecuteScript.invoke(script, BUILD_UI_FUNCTION)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                ScriptResult.Failure(e.message ?: e.javaClass.simpleName)
            }
            if (result is ScriptResult.Failure) {
                val error = result.error
                // A missing buildUI() is fine: the script is not script-driven.
                if (session == uiSession && error?.contains("no function") != true) {
                    mutableState.update { it.copy(log = "buildUI error: $error") }
                }
            }
        }
    }

    /**
     * Pressing a `RoUI.Button`: collects every non-blank input configured through
     * `RoCatUI.addInput`, forwards them as `Map<id, value>` to the named JS function,
     * and renders its return value (or error) in the console area.
     */
    fun onScriptButton(functionName: String) {
        val script = state.value.selectedScript ?: return
        val inputs = uiComponents
            .filterIsInstance<ScriptUIComponent.Input>()
            .filter { it.value.isNotBlank() }
            .associate { it.id to it.value.trim() }

        mutableState.update { it.copy(executing = true, log = "") }
        viewModelScope.launch {
            try {
                val result = uiExecuteScript.invoke(script, functionName, inputs = inputs)
                val message = when (result) {
                    is ScriptResult.Success -> result.value
                    is ScriptResult.Failure -> "Error: ${result.error}"
                }
                mutableState.update {
                    it.copy(
                        executing = false,
                        log = message.ifEmpty { "`$functionName` returned nothing" },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                mutableState.update {
                    it.copy(executing = false, log = "Error: ${e.message ?: e.javaClass.simpleName}")
                }
            }
        }
    }

    /** Updates a single input's value as the user types, keeping the item keyed by id. */
    fun updateInputValue(id: String, value: String) {
        val index = uiComponents.indexOfFirst { (it as? ScriptUIComponent.Input)?.id == id }
        if (index < 0) return
        val input = uiComponents[index] as ScriptUIComponent.Input
        if (input.value == value) return
        uiComponents[index] = input.copy(value = value)
    }

    /** Adds a new input, or refreshes its hint when a script re-declares the same id. */
    private fun addOrReplaceInput(id: String, hint: String) {
        val index = uiComponents.indexOfFirst { it is ScriptUIComponent.Input && (it as ScriptUIComponent.Input).id == id }
        if (index >= 0) {
            val current = uiComponents[index] as ScriptUIComponent.Input
            if (current.hint != hint) uiComponents[index] = current.copy(hint = hint)
        } else {
            uiComponents.add(ScriptUIComponent.Input(id, hint))
        }
    }

    /** Marshals a UI mutation to the main thread and drops it if it belongs to a stale render. */
    private fun postUi(session: Long, block: () -> Unit) {
        viewModelScope.launch {
            if (session != uiSession) return@launch
            block()
        }
    }

    private companion object {
        const val BUILD_UI_FUNCTION = "buildUI"
    }
}