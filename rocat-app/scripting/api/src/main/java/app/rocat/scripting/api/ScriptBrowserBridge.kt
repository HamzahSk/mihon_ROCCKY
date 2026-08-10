package app.rocat.scripting.api

/**
 * Headless-browser bridge exposed to user scripts as the global `RoCatPage` object
 * (Tahap 23: dual-mode scraping engine). It mirrors the Puppeteer subset RoCat scripts
 * need to break form logins, anti-bot challenges and JS-generated player iframes that a
 * plain `fetch()` + Jsoup parse cannot reach.
 *
 * **Threading contract:** the Rhino engine evaluates scripts on a background coroutine,
 * so every method here is **synchronous and blocking**. Implementations marshal their
 * work onto the Android main thread (a WebView is main-thread bound) and park the
 * calling thread until a result is ready. Only the background script thread is blocked —
 * the main UI thread stays responsive.
 *
 * **Dual-mode guidance:** prefer `fetch()` + `RoCatDOM` (Mode Statis) for plain HTML
 * scraping — it is cheap, fast and battery friendly. Reach for [open]/[type]/[click]/
 * [waitForSelector]/[evaluate] only when the target genuinely needs a live browser
 * (JS-rendered DOM, form submission, anti-bot walls), because rendering a real page is
 * far heavier than an HTTP request.
 *
 * All failures are reported through return values (e.g. `false`, empty string), never
 * thrown, so a misbehaving page cannot crash the script.
 */
interface ScriptBrowserBridge {

    /**
     * Opens [url] in the hidden WebView and blocks until the page finished loading or
     * [timeoutMs] elapses.
     *
     * @return `true` when the page reached `onPageFinished` (or reported a load error)
     *   within the timeout; `false` on timeout / no browser available.
     */
    fun open(url: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean

    /**
     * Fills the first element matching [selector] with [text], dispatching real
     * `focus`/`input`/`change`/`blur` events so React/Vue-style frameworks pick the
     * value up.
     *
     * @return `true` when the element existed and was filled.
     */
    fun type(selector: String, text: String): Boolean

    /**
     * Dispatches a real pointer/mouse `click` sequence on the first element matching
     * [selector] (falls back to `element.click()`).
     *
     * @return `true` when the element existed and was clicked.
     */
    fun click(selector: String): Boolean

    /**
     * Polls the live DOM until an element matching [selector] exists or [timeoutMs]
     * elapses. Useful after a click that triggers an async navigation/re-render.
     *
     * @return `true` when the selector appeared within the timeout.
     */
    fun waitForSelector(selector: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean

    /**
     * Runs [script] inside the page's own JavaScript context and returns the value the
     * browser produced as a **JSON-encoded string** (WebView's `evaluateJavascript`
     * contract). The Rhino bridge parses that string back into a native JS value; when
     * it is not JSON (e.g. the literal `undefined`) the raw string is returned instead.
     *
     * Returns `"null"` when no page is open or the script could not run.
     */
    fun evaluate(script: String): String

    /** Returns the fully-rendered current HTML (`document.documentElement.outerHTML`). */
    fun getHtml(): String

    /** Releases the hidden WebView and frees its memory. No-op when no page is open. */
    fun close()

    companion object {
        /** Default per-call timeout used by `open` / `waitForSelector`. */
        const val DEFAULT_TIMEOUT_MS: Long = 15_000L
    }
}
