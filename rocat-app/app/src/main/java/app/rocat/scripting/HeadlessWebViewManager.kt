package app.rocat.scripting

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import app.rocat.core.common.util.WebViewUtil
import org.json.JSONTokener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Manages a single **headless** [WebView] (Tahap 23: dual-mode scraping engine) that
 * backs the script-facing `RoCatPage` global.
 *
 * WebView is Android main-thread bound, while the Rhino engine runs scripts on a
 * background thread. Every public method therefore marshals its work onto the main
 * looper via [Handler] and blocks the calling thread with a [CountDownLatch] until a
 * result is ready. The main UI thread is never blocked — only the script thread parks.
 *
 * The WebView is created lazily on first use and torn down by [close]; the instance is
 * never attached to a view hierarchy, so nothing leaks into the UI.
 */
class HeadlessWebViewManager(private val appContext: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var webView: WebView? = null

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    /** Creates the hidden WebView on the main thread if it does not exist yet. */
    @SuppressLint("SetJavaScriptEnabled")
    private fun ensureWebView(): WebView? {
        webView?.let { return it }
        val latch = CountDownLatch(1)
        val ref = AtomicReference<WebView?>()
        onMain {
            try {
                val wv = WebView(appContext)
                WebViewUtil.setDefaultSettings(wv)
                wv.setBackgroundColor(Color.TRANSPARENT)
                wv.webViewClient = WebViewClient()
                webView = wv
                ref.set(wv)
            } catch (_: Throwable) {
                ref.set(null)
            }
            latch.countDown()
        }
        if (!latch.await(5, TimeUnit.SECONDS)) return null
        return ref.get()
    }

    /**
     * Opens [url] and blocks until `onPageFinished` (or a load error) fires or
     * [timeoutMs] elapses.
     */
    fun open(url: String, timeoutMs: Long): Boolean {
        val wv = ensureWebView() ?: return false
        val latch = CountDownLatch(1)
        onMain {
            try {
                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) = latch.countDown()
                }
                wv.loadUrl(url)
            } catch (_: Throwable) {
                latch.countDown()
            }
        }
        return latch.await(timeoutMs, TimeUnit.MILLISECONDS)
    }

    /** Fills the element matching [selector] with [text] (React/Vue-friendly events). */
    fun type(selector: String, text: String): Boolean {
        val js = """
            (function() {
                var el = document.querySelector(${jsQuote(selector)});
                if (!el) return false;
                try { el.focus(); } catch (e) {}
                el.value = ${jsQuote(text)};
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                try { el.dispatchEvent(new Event('blur', { bubbles: true })); } catch (e) {}
                return true;
            })()
        """.trimIndent()
        return evaluateJs(js, DEFAULT_EVAL_TIMEOUT_MS) == "true"
    }

    /** Dispatches a real pointer/mouse click sequence on [selector]. */
    fun click(selector: String): Boolean {
        val js = """
            (function() {
                var el = document.querySelector(${jsQuote(selector)});
                if (!el) return false;
                var opts = { bubbles: true, cancelable: true, view: window };
                try {
                    el.dispatchEvent(new MouseEvent('pointerdown', opts));
                    el.dispatchEvent(new MouseEvent('mousedown', opts));
                    el.dispatchEvent(new MouseEvent('pointerup', opts));
                    el.dispatchEvent(new MouseEvent('mouseup', opts));
                    el.dispatchEvent(new MouseEvent('click', opts));
                } catch (e) {
                    el.click();
                }
                return true;
            })()
        """.trimIndent()
        return evaluateJs(js, DEFAULT_EVAL_TIMEOUT_MS) == "true"
    }

    /** Polls the live DOM until [selector] exists or [timeoutMs] elapses. */
    fun waitForSelector(selector: String, timeoutMs: Long): Boolean {
        val probe = "(document.querySelector(${jsQuote(selector)}) !== null)"
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            if (evaluateJs(probe, DEFAULT_EVAL_TIMEOUT_MS) == "true") return true
            if (System.currentTimeMillis() >= deadline) return false
            try {
                Thread.sleep(POLL_INTERVAL_MS)
            } catch (_: InterruptedException) {
                return false
            }
        }
    }

    /** Runs [script] in the live page and returns the raw JSON-encoded result. */
    fun evaluate(script: String): String =
        evaluateJs(script, DEFAULT_EVAL_TIMEOUT_MS) ?: "null"

    /** Returns the current fully-rendered HTML (JSON-decoded). */
    fun getHtml(): String {
        val raw = evaluateJs("document.documentElement.outerHTML", DEFAULT_EVAL_TIMEOUT_MS) ?: return ""
        return unquoteJson(raw)
    }

    /** Releases the hidden WebView and frees its memory (safe to call repeatedly). */
    fun close() {
        onMain {
            val wv = webView
            webView = null
            wv?.let { view ->
                try {
                    view.stopLoading()
                    view.removeAllViews()
                    view.destroy()
                } catch (_: Throwable) {
                    // Already destroyed — nothing else to free.
                }
            }
        }
    }

    /** Runs [js] on the main thread and returns the callback value, or null on failure. */
    private fun evaluateJs(js: String, timeoutMs: Long): String? {
        val wv = webView ?: ensureWebView() ?: return null
        val latch = CountDownLatch(1)
        val ref = AtomicReference<String?>()
        onMain {
            try {
                wv.evaluateJavascript(js) { value ->
                    ref.set(value)
                    latch.countDown()
                }
            } catch (_: Throwable) {
                ref.set(null)
                latch.countDown()
            }
        }
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) return null
        return ref.get()
    }

    /** Decodes WebView's JSON-encoded callback string back into a plain string. */
    private fun unquoteJson(value: String): String = try {
        JSONTokener(value).nextValue() as? String ?: value
    } catch (_: Exception) {
        value
    }

    private companion object {
        const val DEFAULT_EVAL_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 150L
    }
}

/** Quotes a Kotlin string for safe injection into a JS string literal. */
private fun jsQuote(value: String): String =
    "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t") + "\""
