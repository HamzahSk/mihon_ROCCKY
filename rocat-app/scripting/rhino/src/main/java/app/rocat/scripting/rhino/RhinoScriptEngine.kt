package app.rocat.scripting.rhino

import app.rocat.core.common.network.GET
import app.rocat.core.common.network.HeadersBuilder
import app.rocat.core.common.network.POST
import app.rocat.core.common.network.awaitSuccessString
import app.rocat.core.common.network.jsonBody
import app.rocat.scripting.api.FetchResult
import app.rocat.scripting.api.ScriptEngine
import app.rocat.scripting.api.ScriptEnvironment
import app.rocat.scripting.api.ScriptResult
import app.rocat.scripting.api.model.Script
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

/**
 * [ScriptEngine] backed by Mozilla Rhino (a pure-JVM JavaScript engine, chosen over
 * native QuickJS so the default build has no native dependencies).
 */
class RhinoScriptEngine(
    private val client: OkHttpClient,
) : ScriptEngine {

    override val name: String = "Rhino"

    override suspend fun execute(script: Script, environment: ScriptEnvironment): ScriptResult =
        withContext(Dispatchers.IO) {
            try {
                val value = evaluate(script, environment)
                ScriptResult.Success(value)
            } catch (e: Exception) {
                ScriptResult.Failure(e.message ?: e.javaClass.simpleName)
            }
        }

    private fun evaluate(script: Script, environment: ScriptEnvironment): String {
        val rhino = Context.enter()
        return try {
            rhino.optimizationLevel = -1 // interpretive mode is safer for sandboxed scripts
            rhino.languageVersion = Context.VERSION_ES6
            val scope = rhino.initStandardObjects()

            // Expose a synchronous-looking fetch() the script can call. Internally it
            // bridges to the OkHttp stack through runBlocking (bounded by the per-call
            // timeouts set on the dedicated script client).
            val fetchFn = BridgeFetch { url, method, headerMap, body ->
                runFetch(url, method, headerMap, body)
            }
            ScriptableObject.putProperty(scope, "fetch", fetchFn)

            if (environment.document != null) {
                val docWrapper = Context.javaToJS(environment.document, scope)
                ScriptableObject.putProperty(scope, "document", docWrapper)
            }

            // Call the script's main() entry point if present.
            val evaluated = rhino.evaluateString(scope, script.source, script.name, 1, null)
            val mainFn = scope.get("main", scope)
            if (mainFn is Function) {
                val result = mainFn.call(rhino, scope, scope, emptyArray())
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
        return try {
            val request = when (method.uppercase()) {
                "POST" -> POST(url, HeadersBuilder().addAll(headers), body?.let { jsonBody(it) }, cacheControl = null)
                else -> GET(url, HeadersBuilder().addAll(headers), cacheControl = null)
            }
            val call = client.newCall(request)
            val bodyString = kotlinx.coroutines.runBlocking { call.awaitSuccessString() }
            FetchResult(status = 200, headers = emptyMap(), body = bodyString)
        } catch (e: Exception) {
            FetchResult(status = 0, headers = emptyMap(), body = "Fetch failed: ${e.message}")
        }
    }
}

private typealias FetchBridge = (String, String, Map<String, String>, String?) -> FetchResult

/** A Rhino [BaseFunction] that calls back into Kotlin to perform an HTTP fetch. */
private class BridgeFetch(private val bridge: FetchBridge) : BaseFunction() {
    override fun call(
        cx: Context,
        scope: Scriptable,
        thisObj: Scriptable,
        args: Array<out Any?>,
    ): Any? {
        val url = args.getOrNull(0) as? String ?: return null
        val method = (args.getOrNull(1) as? String) ?: "GET"
        @Suppress("UNCHECKED_CAST")
        val headers = args.getOrNull(2) as? Map<*, *>
        val body = args.getOrNull(3) as? String

        val headerMap = LinkedHashMap<String, String>()
        headers?.forEach { (k, v) -> if (k != null && v != null) headerMap[k.toString()] = v.toString() }

        val result = bridge(url, method, headerMap, body)
        return Context.getCurrentContext().let { cx ->
            val obj = cx.newObject(scope)
            ScriptableObject.putProperty(obj, "status", result.status)
            ScriptableObject.putProperty(obj, "body", result.body)
            if (result.headers.isNotEmpty()) {
                val headersJs = cx.newObject(scope)
                result.headers.forEach { (k, v) -> ScriptableObject.putProperty(headersJs, k, v) }
                ScriptableObject.putProperty(obj, "headers", headersJs)
            }
            obj
        }
    }
}