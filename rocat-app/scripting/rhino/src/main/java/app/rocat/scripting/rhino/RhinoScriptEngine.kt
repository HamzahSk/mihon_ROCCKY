package app.rocat.scripting.rhino

import app.rocat.scripting.api.FetchResult
import app.rocat.scripting.api.ScriptEngine
import app.rocat.scripting.api.ScriptEnvironment
import app.rocat.scripting.api.ScriptResult
import app.rocat.scripting.api.model.Script
import app.rocat.scripting.api.network.scriptFetch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.json.JsonParser

/**
 * [ScriptEngine] backed by Mozilla Rhino (a pure-JVM JavaScript engine, chosen over
 * native QuickJS so the default build has no native dependencies).
 *
 * Scripts are executed in interpretive mode behind an instruction budget so infinite
 * loops are aborted, and every `fetch()` goes through the app's OkHttp stack with
 * per-call timeouts so a hanging script cannot take the app down.
 *
 * Rhino 1.7.15 does not support `async`/`await`, so `fetch()` returns a synchronous
 * Response-like object exposing `status`, `ok`, `headers`, `body`, `text()` and
 * `json()`.
 */
class RhinoScriptEngine(
    private val client: OkHttpClient,
    private val instructionBudget: Long = DEFAULT_INSTRUCTION_BUDGET,
) : ScriptEngine {

    override val name: String = "Rhino"

    private val contextFactory: ContextFactory = ScriptContextFactory(instructionBudget)

    override suspend fun execute(
        script: Script,
        environment: ScriptEnvironment,
        args: List<String>,
    ): ScriptResult = withContext(Dispatchers.IO) {
        try {
            ScriptResult.Success(evaluate(script, environment, args))
        } catch (e: EvaluatorException) {
            // Syntax/compile errors are reported back to the UI, never rethrown.
            ScriptResult.Failure("JS error: ${e.message}")
        } catch (e: StackOverflowError) {
            ScriptResult.Failure("Script caused a stack overflow (possible infinite recursion)")
        } catch (e: Exception) {
            ScriptResult.Failure(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun evaluate(script: Script, environment: ScriptEnvironment, args: List<String>): String {
        val cx = contextFactory.enterContext()
        return try {
            cx.setLanguageVersion(Context.VERSION_ES6)
            val scope = cx.initStandardObjects()

            // Expose fetch(); internally it bridges to OkHttp (see runFetch).
            val fetchFn = BridgeFetch { url, method, headers, body ->
                runFetch(url, method, headers, body)
            }
            ScriptableObject.putProperty(scope, "fetch", fetchFn)

            if (environment.document != null) {
                val docWrapper = Context.javaToJS(environment.document, scope)
                ScriptableObject.putProperty(scope, "document", docWrapper)
            }

            val evaluated = cx.evaluateString(scope, script.source, script.name, 1, null)
            val mainFn = scope.get("main", scope)
            if (mainFn is Function) {
                val jsArgs = args.map { Context.javaToJS(it, scope) }.toTypedArray()
                val result = mainFn.call(cx, scope, scope, jsArgs)
                return result?.toString() ?: ""
            }
            evaluated?.toString() ?: ""
        } finally {
            Context.exit()
        }
    }

    private fun runFetch(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
    ): FetchResult {
        val mime = headers["Content-Type"] ?: "application/json; charset=utf-8"
        return try {
            runBlocking { client.scriptFetch(url, method, headers, body, mime) }
        } catch (e: Exception) {
            FetchResult(status = 0, headers = emptyMap(), body = "", error = e.message ?: e.javaClass.simpleName)
        }
    }

    companion object {
        /**
         * Interpreter instruction budget per script run. Generous enough for real
         * scraping loops but small enough to abort an infinite `while(true)`.
         */
        const val DEFAULT_INSTRUCTION_BUDGET: Long = 10_000_000L
    }
}

/**
 * Rhino [ContextFactory] whose contexts abort execution after [instructionBudget]
 * interpreted instructions. Used as a watchdog against runaway / infinite scripts.
 */
private class ScriptContextFactory(private val instructionBudget: Long) : ContextFactory() {
    override fun makeContext(): Context {
        return object : Context(this@ScriptContextFactory) {
            private var instructionCounter = 0L

            override fun observeInstructionCount(instructionCount: Int) {
                instructionCounter += instructionCount
                if (instructionCounter > instructionBudget) {
                    throw EvaluatorException("Script execution timed out (instruction budget exceeded)")
                }
            }
        }.apply {
            setInstructionObserverThreshold(10_000)
        }
    }
}

private typealias FetchBridge = (String, String, Map<String, String>, String?) -> FetchResult

/**
 * A Rhino [BaseFunction] that performs an HTTP request through Kotlin and returns a
 * Response-like object: `{ status, statusText, ok, url, body, headers, text(), json() }`.
 *
 * Accepts either the positional form `fetch(url, "POST", headers, body)` or the
 * standard options form `fetch(url, { method, headers, body })`.
 */
private class BridgeFetch(
    private val bridge: FetchBridge,
) : BaseFunction() {

    override fun call(
        cx: Context,
        scope: Scriptable,
        thisObj: Scriptable,
        args: Array<out Any?>,
    ): Any? {
        val url = args.getOrNull(0) as? String ?: return null

        var method = "GET"
        var headers = emptyMap<String, String>()
        var body: String? = null

        when (val second = args.getOrNull(1)) {
            is String -> method = second
            is Scriptable -> {
                method = getStringProperty(second, "method") ?: method
                headers = getStringMap(second.get("headers", second))
                body = getStringProperty(second, "body")
            }
        }
        val third = args.getOrNull(2)
        if (third is Scriptable) {
            headers = getStringMap(third)
        }
        val fourth = args.getOrNull(3)
        if (fourth is String) body = fourth

        val result = bridge(url, method, headers, body)

        val obj = cx.newObject(scope)
        ScriptableObject.putProperty(obj, "status", result.status)
        ScriptableObject.putProperty(obj, "statusText", result.statusText)
        ScriptableObject.putProperty(obj, "ok", result.ok)
        ScriptableObject.putProperty(obj, "url", url)
        ScriptableObject.putProperty(obj, "body", result.body)
        ScriptableObject.putProperty(obj, "error", result.error)

        val headersJs = cx.newObject(scope)
        result.headers.forEach { (key, value) -> ScriptableObject.putProperty(headersJs, key, value) }
        ScriptableObject.putProperty(obj, "headers", headersJs)

        ScriptableObject.putProperty(obj, "text", SyncValueFn { result.body })
        ScriptableObject.putProperty(obj, "json", SyncValueFn {
            if (result.body.isBlank()) throw EvaluatorException("Empty response body cannot be parsed as JSON")
            JsonParser(cx, scope).parseValue(result.body)
        })
        return obj
    }

    private fun getStringProperty(sb: Scriptable, name: String): String? {
        val value = sb.get(name, sb) ?: return null
        if (value is Undefined) return null
        return Context.toString(value)
    }

    private fun getStringMap(value: Any?): Map<String, String> {
        if (value !is Scriptable) return emptyMap()
        val map = LinkedHashMap<String, String>()
        for (id in value.ids) {
            val key = id.toString()
            val v = value.get(key, value)
            if (v != null && v !== Undefined.instance) map[key] = Context.toString(v)
        }
        return map
    }
}

/** A Rhino function that lazily produces (and returns) a single value. */
private class SyncValueFn(private val supplier: () -> Any?) : BaseFunction() {
    override fun call(
        cx: Context,
        scope: Scriptable,
        thisObj: Scriptable,
        args: Array<out Any?>,
    ): Any? = supplier()
}
