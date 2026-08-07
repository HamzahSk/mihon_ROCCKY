package app.rocat.scripting.api

import app.rocat.scripting.api.model.Script

/**
 * The execution context handed to a running script. Provides the JS code access to
 * the primitives it needs: an HTTP `fetch` and (optionally) a document to manipulate.
 */
interface ScriptEnvironment {
    /**
     * Executes an HTTP request from inside JavaScript. Implementations bridge into
     * the app's OkHttp stack via [app.rocat.core.common.network] primitives.
     */
    suspend fun fetch(
        url: String,
        method: String = "GET",
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
    ): FetchResult

    /** Optional DOM-ish object a script can manipulate; `null` when unavailable. */
    val document: Any?
}

/**
 * Result of a [ScriptEnvironment.fetch] call, serializable so it can cross from the
 * JS evaluation context back into Kotlin world.
 */
data class FetchResult(
    val status: Int,
    val headers: Map<String, String>,
    val body: String,
)

/**
 * Contract for any JavaScript engine used to run user scripts. Kept intentionally
 * small so multiple engines (Rhino, QuickJS, J2V8) can be swapped in behind the same
 * interface - analogous to how mihon abstracts its extensions behind `SourceApi`.
 */
interface ScriptEngine {
    val name: String

    /**
     * Executes [script]'s `main` entry point with the given [environment].
     *
     * @return the value the script returns (typically the JSON-serialisable result of
     *   its work, e.g. an array of scraped items).
     */
    suspend fun execute(script: Script, environment: ScriptEnvironment): ScriptResult
}

/**
 * Outcome of a script execution.
 */
sealed interface ScriptResult {
    data class Success(val value: String) : ScriptResult
    data class Failure(val error: String) : ScriptResult
}