package app.rocat.ui.canvas

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import app.rocat.core.common.injekt.Injekt
import app.rocat.core.viewmodel.StateViewModel
import app.rocat.data.script.ScriptManager
import app.rocat.domain.script.ExecuteScript
import app.rocat.domain.script.GetScripts
import app.rocat.scripting.api.ScriptResult
import app.rocat.scripting.api.ScriptUiBridge
import app.rocat.scripting.api.model.Script
import app.rocat.ui.playground.ScriptUIComponent
import app.rocat.ui.playground.parseGrid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

/**
 * The engine behind the [ScriptCanvasScreen]: a per-script, mihon-like "extension tab".
 *
 * Unlike the shared playground picker, this screen owns exactly one script. When it loads
 * (and again on every script source change) it clears the canvas and invokes the script's
 * `onLaunch()` function, letting the script draw its own initial UI through the global
 * `RoCatUI` object. From then on every tap/branch is script-driven:
 *
 *  - `RoCatUI.addInput`/`addButton` rebuild search forms; pressing a button forwards the
 *    collected inputs to the named JS function.
 *  - `RoCatUI.addGrid` renders a (mihon-style) media grid whose tiles call back into JS
 *    (JSON payload as a string) — the script then "navigates" by calling `RoCatUI.clear()`
 *    and redrawing, e.g. a Search list -> Manga Detail flow.
 *
 * Bridge callbacks are marshalled to the main thread and guarded by a session token so a
 * stale render can never wipe a newer one. Returns of `null`/`undefined` handlers are
 * flattened to empty so the console only shows real output/errors.
 */
class ScriptCanvasViewModel(
    private val scriptId: String,
    private val getScripts: GetScripts = Injekt.get(),
    private val scriptManager: ScriptManager = Injekt.get(),
) : StateViewModel<ScriptCanvasViewModel.State>(State()) {

    data class State(
        val script: Script? = null,
        val loaded: Boolean = false,
        val executing: Boolean = false,
        val output: String = "",
    )

    /** The ordered, script-driven list of components rendered by the canvas. */
    val uiComponents: SnapshotStateList<ScriptUIComponent> = mutableStateListOf()

    /**
     * Monotonic session id. Incremented whenever a fresh render starts (a new `onLaunch()`
     * draw or a script source change) so queued bridge updates from an older render are
     * discarded on the main thread.
     */
    @Volatile
    private var uiSession: Long = 0

    /** Last source string that triggered a render; used to auto-redraw on edit. */
    private var lastSource: String? = null

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
        override fun addGrid(columns: Int, itemsJsonString: String, onClickFunction: String) = postUi(uiSession) {
            parseGrid(columns, itemsJsonString, onClickFunction)?.let { uiComponents.add(it) }
        }
        override fun log(text: String) = postUi(uiSession) {
            uiComponents.add(ScriptUIComponent.LogText(text))
        }
    }

    /** The engine/environment pair used for every script-driven invocation. */
    private val uiExecuteScript: ExecuteScript by lazy {
        ExecuteScript(
            engine = scriptManager.engine,
            environment = scriptManager.createEnvironment(uiBridge),
        )
    }

    init {
        viewModelScope.launch {
            getScripts.subscribe().collect { list ->
                val script = list.firstOrNull { it.id == scriptId }
                mutableState.update { it.copy(script = script, loaded = true) }
                if (script != null && script.source != lastSource) {
                    lastSource = script.source
                    renderOnLaunch(script)
                }
            }
        }
    }

    /**
     * Starts a fresh canvas render: clears the previous components and invokes the
     * script's `onLaunch()` function which repopulates the UI via `RoCatUI.*`.
     */
    private fun renderOnLaunch(script: Script) {
        uiSession++
        val session = uiSession
        postUi(session) { uiComponents.clear() }
        mutableState.update { it.copy(output = "") }

        viewModelScope.launch {
            val result = try {
                uiExecuteScript.invoke(script, ON_LAUNCH_FUNCTION)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                ScriptResult.Failure(e.message ?: e.javaClass.simpleName)
            }
            if (session != uiSession) return@launch
            if (result is ScriptResult.Failure) {
                val error = result.error
                // A missing onLaunch() is fine: such scripts are not canvas-driven.
                if (error?.contains("no function") != true) {
                    mutableState.update {
                        it.copy(output = "onLaunch error: $error")
                    }
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

    /**
     * Pressing a `RoCatUI.Button`: gathers every non-blank input into a `Map<id, value>`
     * and invokes the named JS function with that object as a single argument.
     */
    fun onScriptButton(functionName: String) {
        val script = state.value.script ?: return
        val inputs = uiComponents
            .filterIsInstance<ScriptUIComponent.Input>()
            .filter { it.value.isNotBlank() }
            .associate { it.id to it.value.trim() }
        execute(script, functionName, inputs = inputs, args = emptyList())
    }

    /**
     * Tapping a tile of a `RoCatUI` grid: forwards the tile's raw JSON payload as a
     * string argument (`openDetail(itemJson)`) so the script can render its detail page.
     */
    fun onGridItemClick(functionName: String, payload: String) {
        val script = state.value.script ?: return
        execute(script, functionName, inputs = emptyMap(), args = listOf(payload))
    }

    /** Re-runs the script's `onLaunch()` to redraw the canvas from scratch. */
    fun rebuildCanvas() {
        state.value.script?.let { renderOnLaunch(it) }
    }

    private fun execute(
        script: Script,
        functionName: String,
        inputs: Map<String, String>,
        args: List<String>,
    ) {
        mutableState.update { it.copy(executing = true, output = "") }
        viewModelScope.launch {
            val result = try {
                if (args.isNotEmpty()) {
                    uiExecuteScript.invoke(script, functionName, args = args)
                } else {
                    uiExecuteScript.invoke(script, functionName, inputs = inputs)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                ScriptResult.Failure(e.message ?: e.javaClass.simpleName)
            }

            val message = when (result) {
                is ScriptResult.Success -> normalizeOutput(result.value)
                is ScriptResult.Failure -> "Error: ${result.error}"
            }
            mutableState.update { it.copy(executing = false, output = message) }
        }
    }

    /** Remembers the id and refreshes its hint when the script re-declares the same id. */
    private fun addOrReplaceInput(id: String, hint: String) {
        val index = uiComponents.indexOfFirst {
            it is ScriptUIComponent.Input && (it as ScriptUIComponent.Input).id == id
        }
        if (index >= 0) {
            val current = uiComponents[index] as ScriptUIComponent.Input
            if (current.hint != hint) uiComponents[index] = current.copy(hint = hint)
        } else {
            uiComponents.add(ScriptUIComponent.Input(id, hint))
        }
    }

    /** Marshals a UI mutation to the main thread and drops it if it is a stale render. */
    private fun postUi(session: Long, block: () -> Unit) {
        viewModelScope.launch {
            if (session != uiSession) return@launch
            block()
        }
    }

    private companion object {
        const val ON_LAUNCH_FUNCTION = "onLaunch"

        /** Flattens `null`/`undefined`/blank handler returns so the console stays clean. */
        fun normalizeOutput(value: String): String = when {
            value.isBlank() || value == "null" || value == "undefined" -> ""
            else -> value
        }
    }

    /**
     * Builds a [ScriptCanvasViewModel] for a specific [scriptId]. Because this ViewModel
     * takes a constructor argument, it cannot go through the reflection-based default
     * factory; the factory closes over the id instead (mirrors ScriptDetailScreen).
     */
    class Factory(private val scriptId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            ScriptCanvasViewModel(scriptId) as T
    }
}