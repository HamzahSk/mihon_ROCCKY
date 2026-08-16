# Tahap 25 — RoCat Browser Automation Engine (General-Purpose Support)

- Tanggal: 2026-08-16
- Sub-tahap: 25.1 native bridge diperluas, 25.2 RoCatBrowserBridge app, 25.3 polyfill JS RoCatBrowser, 25.4 test & docs.
- Status: SELESAI — `:scripting:rhino:testDebugUnitTest` 51 hijau (44 lama + 7 baru), `:app:assembleDebug` SUCCESS.

## Ringkasan

Membangun lapisan **browser automation general-purpose** di atas WebView headless yang
sudah ada (Tahap 23 `RoCatPage`): skrip kini punya API sinkron bergaya
**Playwright/Puppeteer** (`RoCatBrowser`) untuk membuka URL, mengisi form, klik,
menunggu elemen, mengekstrak DOM dinamis, menjalankan JS di konteks halaman,
screenshot, dan manajemen cookie — tanpa mengubah kode aplikasi inti.

## 25.1 — Native bridge diperluas (scripting)

- `scripting/api/.../ScriptBrowserBridge.kt`: +11 method general-purpose, semua
  **default no-op** agar `FakeBrowser` lama & implementasi lain tetap valid:
  - `sleep(ms)`, `url()`, `title()` → `""`
  - `goBack()`, `goForward()`, `reload()`, `stop()` → `false`
  - `waitForLoad(state, timeoutMs)` → `false`
  - `screenshot(path, quality)` → `""`
  - `getCookies()` → `"[]"`, `setCookie(json)` → `false`, `clearCookies()` → `false`
- `scripting/rhino/.../RoCatPageBridge.kt`: +11 `put` primitif low-level + helper
  `pageArgInt` (baru). `sleep` terima Long, `screenshot(path, quality)`.
- `scripting/rhino/.../RhinoScriptEngine.kt`: inject `RO_CAT_BROWSER_WRAPPER_JS`
  SETELAH `RoCatPage` & SEBELUM kode user, hanya saat `environment.browser != null`.

## 25.2 — App layer (RoCatBrowserBridge)

- **Baru** `app/src/main/java/app/rocat/scripting/RoCatBrowserBridge.kt`: implementasi
  penuh `ScriptBrowserBridge` (delegasi ke `HeadlessWebViewManager`, semua `runCatching`).
  Menggantikan `AppScriptBrowserBridge.kt` (**dihapus**); `AppModule` mendaftarkan
  `RoCatBrowserBridge` sebagai `ScriptBrowserBridge`.
- `HeadlessWebViewManager.kt`: implementasi nyata:
  - `sleep` = `Thread.sleep` thread skrip (tanpa main-thread hop).
  - `url`/`title` via `evaluateJs("location.href" / "document.title")` + `unquoteJson`.
  - `goBack/goForward/reload/stop` via `navigate {}` (main-thread latch).
  - `waitForLoad` poll `document.readyState` (`load`/`complete`→complete,
    `domcontentloaded`/`interactive`→interactive), 150 ms interval.
  - `screenshot`: main-thread `measure`+`layout` (viewport default 1366×768) bila
    `width/height==0` (WebView tak pernah di-attach), `draw(Canvas)` → bitmap, tulis
    PNG ke `path` (bila diisi) atau `cacheDir/browser_screenshots/shot_<ts>.png`;
    encode di thread pemanggil, return absolute path.
  - `getCookies` baca `CookieManager.getInstance().getCookie(url)` → JSON array
    `[{name,value,domain,path,url}]`.
  - `setCookie` terima objek JSON `{name,value,url?,domain?,path?}` ATAU raw
    `"name=value"` (fallback saat `JSONObject` gagal) → `CookieManager.setCookie` + flush.
  - `clearCookies` = `removeAllCookies(null)` + flush.
  - Cookie memakai `CookieManager` = store yang sama dengan `AndroidCookieJar` →
    otomatis sync dengan `fetch()` OkHttp.

## 25.3 — Polyfill JS `RoCatBrowser` (scripting/rhino)

- **Baru** `RoCatBrowserWrapper.kt` (`RO_CAT_BROWSER_WRAPPER_JS`), ES5 Rhino-1.7.15-safe
  (tanpa async/class/spread/optional-chaining), di atas primitif `RoCatPage`:
  - Top-level: `launch/getInstance/connect/version/setDefaultTimeout/hasBrowser`.
  - `Browser`: `launch({headless,viewport})`, `newPage`, `page`, `close`,
    `setDefaultTimeout`.
  - `Page`: `goto(url,{waitUntil,timeout})`, `waitForLoad`, `waitForTimeout`,
    `content`, `evaluate(fn,argsArray)` (fn di-serialize + argumen `JSON.stringify`),
    `url/title`, `click/fill/text/getAttribute`, `waitForSelector`,
    `goBack/goForward/reload/stop`, `screenshot({path,quality})`,
    `cookies/setCookie/clearCookies`, `close`.
  - `Locator`: `click/fill/type(text,delay)/text/getAttribute/exists/waitFor/all/
    clickAll/scrollIntoView/getBoundingRect`.
  - Kontrak error: `click`/`fill` return `{success:false,error}` (tak throw); hanya
    `waitFor`/`waitForSelector`/`goto`/`waitForLoad` yang `throw new Error` (try/catch).
  - `waitForTimeout` memakai `RoCatPage.sleep(ms)` (native) — busy-loop JS berisiko
    watchdog instruksi 10M.
  - Catatan: property internal Page memakai `_url`/`_title` (bukan `url`/`title`) agar
    tidak menimpa method (fix TypeError).

## 25.4 — Test, skrip & docs

- **Baru** `RoCatBrowserAutomationTest.kt` (7 test rhino): registrasi global (undefined
  tanpa bridge, object dengan bridge); launch→newPage→goto→title/url/locator; evaluate
  fn+args; all/clickAll/getBoundingRect/getAttribute/scrollIntoView; navigasi+cookies+
  screenshot+sleep; missing element graceful `{success:false}`; waitFor timeout throw.
  `FakeBrowser` baru mengimplementasikan method Tahap 25 dan menjawab `evaluate`
  berdasar marker string script (karena wrapper selalu kirim `(fn)(args)`).
- `capcut_test.js` (root repo): ditulis ulang dari Playwright Node ke skrip RoCat
  memakai `RoCatBrowser` + `RoCat.render`/`RoCatUI` (onLaunch → doCreateAccount →
  json result, error handling, screenshot & cookies).
- **Baru** `DOCS_BROWSER_API.md` (root repo): overview, kapan pakai, kontrak threading,
  API reference (Browser/Page/Locator), contoh, tabel migrasi Playwright→RoCatBrowser,
  arsitektur & keputusan desain, batasan.
- `DOCS_SCRIPTING.md`: +§4.6 cross-ref `RoCatBrowser` (Tahap 25) → `DOCS_BROWSER_API.md`.

## Build & Test

- `sh gradlew :scripting:rhino:testDebugUnitTest` → **51 test hijau** (44 lama + 7 baru).
- `sh gradlew :app:assembleDebug` → SUCCESS.

## Catatan

- Method baru di interface bridge wajib default no-op (pola sama `ScriptUiBridge`).
- Screenshot butuh `measure`+`layout` karena WebView headless berukuran 0×0.
- `RoCatBrowser` (dan `RoCatPage`) hanya ter-inject bila host menyuplai bridge browser.