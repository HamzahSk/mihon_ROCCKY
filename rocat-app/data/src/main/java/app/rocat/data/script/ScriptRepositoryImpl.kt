package app.rocat.data.script

import app.rocat.core.common.network.GET
import app.rocat.core.common.network.HeadersBuilder
import app.rocat.core.common.network.NetworkHelper
import app.rocat.core.common.network.POST
import app.rocat.core.common.network.awaitSuccessString
import app.rocat.core.common.network.jsonBody
import app.rocat.core.common.util.JsonUtil
import app.rocat.domain.script.ScriptRepository
import app.rocat.scripting.api.FetchResult
import app.rocat.scripting.api.ScriptEngine
import app.rocat.scripting.api.ScriptEnvironment
import app.rocat.scripting.api.ScriptResult
import app.rocat.scripting.api.model.DefaultScriptEnvironment
import app.rocat.scripting.api.model.Script
import app.rocat.scripting.rhino.RhinoScriptEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import java.io.File

/**
 * In-memory script store backed by a JSON file. Mirrors the repository-implementation
 * split: the [ScriptRepository] interface lives in `domain`, the impl in `data`.
 */
class ScriptRepositoryImpl(
    private val storageDir: File,
) : ScriptRepository {

    private val storeFile = File(storageDir, "scripts.json")

    private val _scripts = MutableStateFlow(load())
    override fun getAllScripts(): Flow<List<Script>> = _scripts.asStateFlow()

    private val mutex = Mutex()

    override suspend fun getScriptById(id: String): Script? =
        _scripts.value.firstOrNull { it.id == id }

    override suspend fun upsertScript(script: Script) = mutex.withLock {
        val updated = _scripts.value
            .filterNot { it.id == script.id }
            .plus(script)
            .sortedBy { it.name }
        _scripts.value = updated
        save(updated)
    }

    override suspend fun deleteScript(id: String) = mutex.withLock {
        val updated = _scripts.value.filterNot { it.id == id }
        _scripts.value = updated
        save(updated)
    }

    override suspend fun setEnabled(id: String, enabled: Boolean) = mutex.withLock {
        val updated = _scripts.value.map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        _scripts.value = updated
        save(updated)
    }

    private fun load(): List<Script> {
        return try {
            if (!storeFile.exists()) return emptyList()
            JsonUtil.json.decodeFromString<List<Script>>(storeFile.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save(scripts: List<Script>) {
        try {
            if (!storeFile.parentFile.exists()) storeFile.parentFile.mkdirs()
            storeFile.writeText(JsonUtil.json.encodeToString(scripts))
        } catch (e: Exception) {
            // Best-effort persistence; failures should not crash the app.
        }
    }
}

/**
 * Wires the [RhinoScriptEngine] together with a network-backed [ScriptEnvironment].
 * The engine re-uses the app's OkHttp client via a dedicated script client.
 */
class ScriptManager(
    networkHelper: NetworkHelper,
) {
    /** The shared script client with aggressive timeouts. */
    private val scriptClient: OkHttpClient = networkHelper.newScriptClient()

    val engine: ScriptEngine = RhinoScriptEngine(scriptClient)

    val environment: ScriptEnvironment = DefaultScriptEnvironment(
        fetchImpl = { url: String, method: String, headers: Map<String, String>, body: String? ->
            runFetch(scriptClient, url, method, headers, body)
        },
    )

    private suspend fun runFetch(
        client: OkHttpClient,
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
            val bodyString = call.awaitSuccessString()
            FetchResult(status = 200, headers = emptyMap(), body = bodyString)
        } catch (e: Exception) {
            FetchResult(status = 0, headers = emptyMap(), body = "Fetch failed: ${e.message}")
        }
    }
}