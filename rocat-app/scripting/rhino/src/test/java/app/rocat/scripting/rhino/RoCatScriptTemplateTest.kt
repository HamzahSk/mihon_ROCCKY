package app.rocat.scripting.rhino

import app.rocat.scripting.api.FetchResult
import app.rocat.scripting.api.ScriptResult
import app.rocat.scripting.api.ScriptUiBridge
import app.rocat.scripting.api.model.DefaultScriptEnvironment
import app.rocat.scripting.api.model.Script
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Tahap 22 — Simplification API, universal wrapper (`RoCat`) and the new UI template
 * bridge calls (`addJsonLog` / `addHtmlPreview` / `addAudio` / `addAlert` /
 * `addBadgeGroup`). Verifies the auto-injected core wrapper is fault-tolerant and that
 * every new bridge method is reachable from JS with tolerant argument coercion.
 */
class RoCatScriptTemplateTest {

    private lateinit var server: MockWebServer
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()
    private val engine = RhinoScriptEngine(client)
    private val plainEnvironment = DefaultScriptEnvironment(
        fetchImpl = { _, _, _, _ -> FetchResult(200, emptyMap(), "") },
    )

    private fun script(source: String) = Script(id = "t", name = "t", source = source)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /** Records every new template-card call so tests can assert the JS→bridge path. */
    private class Recorder : ScriptUiBridge {
        val calls = mutableListOf<String>()
        override fun addInput(id: String, hint: String) { calls += "input:$id:$hint" }
        override fun addButton(label: String, functionName: String) { calls += "button:$label:$functionName" }
        override fun thumbnailPreview(url: String) { calls += "thumb:$url" }
        override fun videoPreview(url: String) { calls += "video:$url" }
        override fun addImage(url: String, title: String, allowDownload: Boolean) {
            calls += "image:$url:$title:$allowDownload"
        }
        override fun addVideo(url: String, title: String, isStreamHls: Boolean, allowDownload: Boolean) {
            calls += "videoCard:$url:$title:$isStreamHls:$allowDownload"
        }
        override fun clear() { calls += "clear" }
        override fun addGrid(columns: Int, itemsJsonString: String, onClickFunction: String) {
            calls += "grid:$columns:$onClickFunction:$itemsJsonString"
        }
        override fun log(text: String) { calls += "log:$text" }
        override fun saveFile(fileName: String, content: String, mimeType: String): String {
            calls += "save:$fileName:$mimeType"
            return fileName
        }
        override fun addJsonLog(dataJson: String, title: String, allowCopy: Boolean) {
            calls += "jsonLog:$title:$allowCopy:$dataJson"
        }
        override fun addHtmlPreview(htmlContent: String, title: String) {
            calls += "html:$title:$htmlContent"
        }
        override fun addAudio(url: String, title: String, allowDownload: Boolean) {
            calls += "audio:$url:$title:$allowDownload"
        }
        override fun addAlert(message: String, type: String) {
            calls += "alert:$type:$message"
        }
        override fun addBadgeGroup(badgesJson: String) {
            calls += "badges:$badgesJson"
        }
    }

    private fun uiEnvironment(ui: ScriptUiBridge) = DefaultScriptEnvironment(
        fetchImpl = { _, _, _, _ -> FetchResult(200, emptyMap(), "") },
        ui = ui,
    )

    // --- Tahap 22.1: the auto-injected `RoCat` core wrapper ---

    @Test
    fun `rocat render draws a whole canvas from a descriptor list`() = runBlocking {
        val ui = Recorder()
        val env = uiEnvironment(ui)
        val source = """
            function build() {
                RoCat.render([
                    { type: "clear" },
                    { type: "input", id: "q", hint: "Cari..." },
                    { type: "button", label: "Cari", fn: "doSearch" },
                    { type: "alert", message: "Perhatian", level: "warning" },
                    { type: "badges", badges: ["Ongoing", "HD", "Action"] },
                    { type: "json", title: "Data", data: { a: 1, b: "x" }, copy: true }
                ]);
            }
        """.trimIndent()

        val result = engine.invokeNamedFunction(script(source), env, "build", emptyMap())
        assertTrue("build failed: $result", result is ScriptResult.Success)

        assertEquals(
            listOf(
                "clear",
                "input:q:Cari...",
                "button:Cari:doSearch",
                "alert:warning:Perhatian",
            ),
            ui.calls.take(4),
        )
        val badges = ui.calls[4]
        assertTrue("badges call missing, got $badges", badges.startsWith("badges:["))
        assertTrue("badges must carry all chips", badges.contains("Ongoing") && badges.contains("Action"))
        val json = ui.calls[5]
        assertTrue("jsonLog call missing, got $json", json.startsWith("jsonLog:Data:true:{"))
        assertTrue("jsonLog must carry serialized object", json.contains("\"a\":1"))
    }

    @Test
    fun `rocat render accepts a single descriptor object`() = runBlocking {
        val ui = Recorder()
        val env = uiEnvironment(ui)
        val source = """
            function build() {
                RoCat.render({ type: "alert", message: "Tunggu", level: "info" });
                RoCat.render({ type: "image", url: "https://x/img.jpg", title: "Cover", download: false });
                RoCat.render({ type: "log", text: "selesai" });
            }
        """.trimIndent()

        val result = engine.invokeNamedFunction(script(source), env, "build", emptyMap())
        assertTrue("build failed: $result", result is ScriptResult.Success)
        assertEquals(
            listOf(
                "alert:info:Tunggu",
                "image:https://x/img.jpg:Cover:false",
                "log:selesai",
            ),
            ui.calls,
        )
    }

    @Test
    fun `rocat render tolerates null and malformed descriptors`() = runBlocking {
        val ui = Recorder()
        val env = uiEnvironment(ui)
        val source = """
            function build() {
                RoCat.render(null);
                RoCat.render([null, 42, { type: "bogus" }, { type: "input" }]);
                RoCatUI.log("ok");
            }
        """.trimIndent()

        val result = engine.invokeNamedFunction(script(source), env, "build", emptyMap())
        assertTrue("build failed: $result", result is ScriptResult.Success)
        // A type-less input renders with blank id/hint; nothing else should appear.
        assertEquals(listOf("input::", "log:ok"), ui.calls)
    }

    @Test
    fun `rocat safeParseJson never throws and returns fallback`() = runBlocking {
        val source = """
            function main() {
                var ok = RoCat.safeParseJson('{"x": 1}', null);
                var bad = RoCat.safeParseJson('not json', 42);
                var nil = RoCat.safeParseJson(null, "fb");
                var undef = RoCat.safeParseJson(undefined, "ud");
                return ok.x + '|' + bad + '|' + nil + '|' + undef;
            }
        """.trimIndent()

        val result = engine.execute(script(source), plainEnvironment)

        assertEquals(ScriptResult.Success("1|42|fb|ud"), result)
    }

    @Test
    fun `rocat fetchJson returns parsed json on success and null on failure`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"name":"rocat","n":42}"""))
        server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))
        val base = server.url("/").toString()
        val source = """
            function main(u) {
                var ok = RoCat.fetchJson(u + "1");
                var err = RoCat.fetchJson(u + "2");
                var bad = RoCat.fetchJson(u + "3");
                return (ok ? ok.name + ':' + ok.n : 'null') + '|' +
                       (err === null ? 'null' : 'x') + '|' +
                       (bad === null ? 'null' : 'x');
            }
        """.trimIndent()

        val result = engine.execute(script(source), plainEnvironment, listOf(base))

        assertEquals(ScriptResult.Success("rocat:42|null|null"), result)
    }

    // --- Tahap 22.2: expanded UI template bridge calls ---

    @Test
    fun `bridge accepts a native js array for addBadgeGroup`() = runBlocking {
        val ui = Recorder()
        val env = uiEnvironment(ui)
        val source = """
            function build() {
                RoCatUI.addBadgeGroup(["Ongoing", "HD", "Action"]);
                RoCatUI.addBadgeGroup('["a","b"]');
                RoCatUI.addBadgeGroup(null);
            }
        """.trimIndent()

        val result = engine.invokeNamedFunction(script(source), env, "build", emptyMap())
        assertTrue("build failed: $result", result is ScriptResult.Success)
        assertEquals(
            listOf(
                """badges:["Ongoing","HD","Action"]""",
                """badges:["a","b"]""",
                "badges:",
            ),
            ui.calls,
        )
    }

    @Test
    fun `bridge addJsonLog accepts object and string with copy defaults`() = runBlocking {
        val ui = Recorder()
        val env = uiEnvironment(ui)
        val source = """
            function build() {
                RoCatUI.addJsonLog({ title: "X", count: 3 }, "Log", true);
                RoCatUI.addJsonLog('{"raw":1}');
                RoCatUI.addJsonLog({ deep: { n: 7 } }, "Tanpa Copy", false);
            }
        """.trimIndent()

        val result = engine.invokeNamedFunction(script(source), env, "build", emptyMap())
        assertTrue("build failed: $result", result is ScriptResult.Success)
        assertEquals(
            listOf(
                """jsonLog:Log:true:{"title":"X","count":3}""",
                """jsonLog::true:{"raw":1}""",
                """jsonLog:Tanpa Copy:false:{"deep":{"n":7}}""",
            ),
            ui.calls,
        )
    }

    @Test
    fun `bridge addAlert defaults to info and tolerates unknown types`() = runBlocking {
        val ui = Recorder()
        val env = uiEnvironment(ui)
        val source = """
            function build() {
                RoCatUI.addAlert("Pesan info");
                RoCatUI.addAlert("Waspada", "warning");
                RoCatUI.addAlert("Eror", "error");
                RoCatUI.addAlert("Sukses", "success");
                RoCatUI.addAlert("Aneh", "bogus");
            }
        """.trimIndent()

        val result = engine.invokeNamedFunction(script(source), env, "build", emptyMap())
        assertTrue("build failed: $result", result is ScriptResult.Success)
        // The bridge passes the type through untouched (never throws); the rendered card
        // is the one that falls back to "info" for unknown types (AlertType.from).
        assertEquals(
            listOf(
                "alert:info:Pesan info",
                "alert:warning:Waspada",
                "alert:error:Eror",
                "alert:success:Sukses",
                "alert:bogus:Aneh",
            ),
            ui.calls,
        )
    }

    @Test
    fun `bridge renders html preview and audio cards`() = runBlocking {
        val ui = Recorder()
        val env = uiEnvironment(ui)
        val source = """
            function build() {
                RoCatUI.addHtmlPreview("<b>Bold</b> text", "Judul");
                RoCatUI.addAudio("https://example.com/track.mp3", "Lagu", true);
                RoCatUI.addAudio("https://example.com/voice.m4a");
            }
        """.trimIndent()

        val result = engine.invokeNamedFunction(script(source), env, "build", emptyMap())
        assertTrue("build failed: $result", result is ScriptResult.Success)
        assertEquals(
            listOf(
                "html:Judul:<b>Bold</b> text",
                "audio:https://example.com/track.mp3:Lagu:true",
                "audio:https://example.com/voice.m4a::true",
            ),
            ui.calls,
        )
    }
}
