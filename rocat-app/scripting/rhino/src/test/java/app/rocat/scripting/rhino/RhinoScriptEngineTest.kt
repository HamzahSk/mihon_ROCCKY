package app.rocat.scripting.rhino

import app.rocat.scripting.api.FetchResult
import app.rocat.scripting.api.ScriptResult
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

class RhinoScriptEngineTest {

    private lateinit var server: MockWebServer
    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .build()
    private val engine = RhinoScriptEngine(client)
    private val environment = DefaultScriptEnvironment(
        fetchImpl = { _, _, _, _ -> FetchResult(200, emptyMap(), "") },
    )

    private fun script(source: String) = Script(
        id = "test",
        name = "test",
        source = source,
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `executes main with arguments`() = runBlocking {
        val source = "function main(url) { return 'hello ' + url; }"
        val result = engine.execute(script(source), environment, listOf("world"))

        assertEquals(ScriptResult.Success("hello world"), result)
    }

    @Test
    fun `returns last expression when no main`() = runBlocking {
        val result = engine.execute(script("1 + 41"), environment)

        assertEquals(ScriptResult.Success("42"), result)
    }

    @Test
    fun `fetch returns text body`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("hi there"))
        val source = """
            function main(u) {
                var r = fetch(u);
                return r.status + ':' + r.body;
            }
        """.trimIndent()

        val result = engine.execute(script(source), environment, listOf(server.url("/x").toString()))

        assertEquals(ScriptResult.Success("200:hi there"), result)
    }

    @Test
    fun `fetch sends custom headers`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        val source = """
            function main(u) {
                var r = fetch(u, { method: 'GET', headers: { 'User-Agent': 'MyAgent', 'X-Test': '1' } });
                return r.status + ':' + r.body;
            }
        """.trimIndent()

        val result = engine.execute(script(source), environment, listOf(server.url("/h").toString()))

        assertEquals(ScriptResult.Success("200:ok"), result)
        val request = server.takeRequest()
        assertEquals("MyAgent", request.getHeader("User-Agent"))
        assertEquals("1", request.getHeader("X-Test"))
    }

    @Test
    fun `fetch parses json in js`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"name":"rocat","n":42}"""))
        val source = """
            function main(u) {
                var r = fetch(u);
                var o = r.json();
                return o.name + ':' + o.n;
            }
        """.trimIndent()

        val result = engine.execute(script(source), environment, listOf(server.url("/j").toString()))

        assertEquals(ScriptResult.Success("rocat:42"), result)
    }

    @Test
    fun `fetch exposes non-2xx status`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("nope"))
        val source = """
            function main(u) {
                var r = fetch(u);
                return r.status + ':' + r.ok + ':' + r.body;
            }
        """.trimIndent()

        val result = engine.execute(script(source), environment, listOf(server.url("/404").toString()))

        assertEquals(ScriptResult.Success("404:false:nope"), result)
    }

    @Test
    fun `fetch reports invalid url as error instead of crashing`() = runBlocking {
        val source = """
            function main() {
                var r = fetch('::bad::');
                return r.status + ':' + (r.error ? 'err' : 'noerr');
            }
        """.trimIndent()

        val result = engine.execute(script(source), environment)

        assertTrue(result is ScriptResult.Success)
        assertTrue((result as ScriptResult.Success).value.contains("err"))
    }

    @Test
    fun `watchdog stops infinite loop`() = runBlocking {
        val source = "function main() { while (true) {} }"

        val result = engine.execute(script(source), environment)

        assertTrue(result is ScriptResult.Failure)
        assertTrue((result as ScriptResult.Failure).error.contains("timed out"))
    }

    @Test
    fun `supports es6 arrow functions and template literals`() = runBlocking {
        val source = """
            var double = (x) => x * 2;
            function main() {
                return `result=${'$'}{double(21)}`;
            }
        """.trimIndent()

        val result = engine.execute(script(source), environment)

        assertEquals(ScriptResult.Success("result=42"), result)
    }
}
