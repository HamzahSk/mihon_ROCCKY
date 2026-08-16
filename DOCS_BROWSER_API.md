# RoCat Browser Automation Engine — `RoCatBrowser` (Dokumentasi Resmi)

> **Tahap 25** — Browser Automation Engine general-purpose untuk RoCat.
> API sinkron bergaya **Playwright/Puppeteer** yang berjalan di atas satu `WebView`
> tersembunyi — skrip bisa membuka URL, mengisi form, klik tombol, menunggu elemen,
> mengekstrak DOM dinamis, menjalankan JavaScript di konteks halaman, mengambil
> screenshot, dan mengelola cookie — **tanpa mengubah kode aplikasi inti**.

---

## 1. Ringkasan

`fetch()` + `RoCatDOM` (Mode Statis) hanya bisa mengambil HTML mentah; halaman yang
di-render JavaScript (SPA), form login, atau player iframe yang URL-nya dibuat JS tidak
terjangkau. `RoCatBrowser` menjembatani **WebView hidup** ke skrip dengan API yang
meniru Playwright:

```javascript
var browser = RoCatBrowser.launch({ headless: true });
var page = browser.newPage();

page.goto("https://example.com/login", { waitUntil: "domcontentloaded", timeout: 30000 });
page.locator('input[name="username"]').fill("admin");
page.locator('button[type="submit"]').click();
page.waitForSelector(".dashboard", 15000);

var title = page.title();          // "Dashboard"
var html  = page.content();        // DOM yang sudah di-render
var shot  = page.screenshot({ path: "", quality: 80 });   // path PNG

browser.close();
```

### Kapan pakai yang mana?

| Situasi | Pilihan |
|---------|---------|
| HTML server-rendered / JSON API | **Statis** — `fetch()` + `RoCatDOM` (murah, hemat baterai) |
| SPA, infinite-scroll, DOM hasil JS | `RoCatPage.open()` lalu `getHtml()` (interaktif sederhana) |
| Form login, anti-bot, multi-langkah | **`RoCatBrowser`** — `locator().fill/click`, `waitForSelector` |
| Butuh navigasi riwayat / screenshot / cookie | **`RoCatBrowser`** — `goBack/goForward/reload/screenshot/cookies` |

### Tanda tersedia

`RoCatBrowser` (dan `RoCatPage`) hanya di-inject ketika host menyuplai bridge browser:

```javascript
if (typeof RoCatBrowser === "undefined") {
    RoCatUI.addAlert("Browser automation tidak tersedia di host ini.", "warning");
    return;
}
```

### Kontrak threading

**Semua method sinkron untuk skrip.** Engine Rhino berjalan di background coroutine;
setiap panggilan bridge mem-park thread skrip dengan `CountDownLatch` sambil melompat ke
main thread (WebView wajib main-thread bound). UI thread **tidak pernah** terblokir.
Konsekuensi: panggilan `RoCatBrowser.*` hanya boleh dilakukan dari skrip yang dijalankan
engine (canvas/playground), bukan dari konteks lain.

---

## 2. `RoCatBrowser` — Top Level

```javascript
RoCatBrowser.launch(options)        // -> Browser baru (memanggil browser.launch)
RoCatBrowser.getInstance()          // -> Browser singleton (dibuat saat pertama dipanggil)
RoCatBrowser.connect()              // alias getInstance()
RoCatBrowser.version()              // -> "RoCatBrowser v1.0.0"
RoCatBrowser.setDefaultTimeout(ms)  // set timeout global
RoCatBrowser.hasBrowser             // -> boolean (apakah bridge ada)
```

---

## 3. Kelas `Browser`

| Method | Keterangan |
|--------|-----------|
| `launch({ headless, viewport })` | Inisialisasi. `headless` (default `true`), `viewport` `{width,height}`. WebView dibuat **lazy** saat dipakai pertama kali. |
| `newPage()` | Membuat `Page` baru dan menjadikannya `currentPage`. Satu WebView dibagi untuk semua page (navigasi bergantian). |
| `page()` | `Page` aktif; membuat satu otomatis bila belum ada. |
| `close()` | Melepas WebView (`stopLoading` + `destroy`). |
| `setDefaultTimeout(ms)` | Ubah timeout default instance. |

```javascript
var b = RoCatBrowser.launch({ headless: false });
var page = b.newPage();
```

---

## 4. Kelas `Page`

### 4.1 Navigasi

```javascript
page.goto(url, options)                  // -> this (throw pada gagal buka)
// options.waitUntil: "load" | "domcontentloaded" | "interactive" | "complete"
// options.timeout:   ms (default 30000)
page.goBack()                            // -> boolean
page.goForward()                         // -> boolean
page.reload()                            // -> boolean
page.stop()                              // -> boolean
page.close()                             // lepas WebView (alias browser.close)
```

### 4.2 Menunggu

```javascript
page.waitForSelector(selector, timeoutMs)   // -> true / throw Timeout
page.waitForLoad(state, timeoutMs)          // state: "load"|"complete"->complete,
                                            //        "domcontentloaded"|"interactive"->interactive
page.waitForTimeout(ms)                     // sleep thread skrip (native, murah)
```

### 4.3 Info halaman & konten

```javascript
page.url()         // -> string (live dari DOM)
page.title()       // -> string (live dari DOM)
page.content()     // -> string, document.documentElement.outerHTML (DOM sudah di-render)
```

### 4.4 Interaksi langsung (pintasan locator)

```javascript
page.click(selector)      // -> { success: true } | { success: false, error }
page.fill(selector, txt)  // -> { success: ... } (focus/input/change/blur)
page.text(selector)       // -> string (textContent.trim())
page.getAttribute(sel, attr)
page.locator(selector)    // -> Locator
```

### 4.5 Evaluasi JavaScript di konteks halaman

```javascript
page.evaluate(fn, args)   // fn = function atau string; args = array nilai
```

`fn` diserialisasi dan dijalankan **di dalam halaman** (bukan scope Rhino). Nilai
balik di-decode ke native JS (string/number/object/array). Literal `undefined` → `null`.

```javascript
var links = page.evaluate(function () {
    return Array.from(document.querySelectorAll("a")).map(function (a) { return a.href; });
});
var sum = page.evaluate(function (a, b) { return a + b; }, [40, 2]); // 42
```

> Setiap `evaluate` = satu round-trip WebView. Untuk ekstraksi berat, lakukan dalam
> satu ekspresi.

### 4.6 Screenshot

```javascript
page.screenshot({ path: "", quality: 80 })   // -> path absolut PNG, atau "" bila gagal
```

WebView tersembunyi di-*draw* ke bitmap (viewport default 1366×768; file PNG ditulis ke
`path` bila diisi, atau ke `cacheDir/browser_screenshots/shot_<ts>.png`). Kembalikan path
untuk disimpan/ ditampilkan.

### 4.7 Cookie (sinkron dengan `fetch()`)

```javascript
page.cookies()              // -> array [{ name, value, domain, path, url }]
page.setCookie(cookie)      // cookie: { name, value, url?, domain?, path? } atau "name=value"
page.clearCookies()         // -> boolean
```

Cookie memakai `WebView CookieManager` — **store yang sama** dengan `AndroidCookieJar`
untuk OkHttp scraper. Login yang dilakukan lewat `RoCatBrowser` langsung terpakai oleh
`fetch()` dan sebaliknya.

---

## 5. Kelas `Locator`

```javascript
var loc = page.locator("selector CSS");
```

| Method | Keterangan |
|--------|-----------|
| `click()` | Pointer/mouse click lengkap (`pointerdown→mousedown→pointerup→mouseup→click`), fallback `el.click()`. → `{success}` |
| `fill(text)` | `el.value = text` + event `focus/input/change/blur` (React/Vue friendly). → `{success}` |
| `type(text, delay)` | Isi per-karakter dengan delay (ms, default 50) antara tiap karakter. → `{success}` |
| `text()` | `textContent.trim()` → string |
| `getAttribute(name)` | → string / `null` |
| `exists()` | → boolean |
| `waitFor(timeoutMs)` | Poll sampai muncul atau **throw** `Timeout waiting for selector: …` |
| `all()` | Array `[{ text, html, attributes:{} }]` semua elemen yang cocok |
| `clickAll()` | Klik semua elemen → array `[{ index, success, error? }]` |
| `scrollIntoView()` | Scroll elemen ke tengah viewport → boolean |
| `getBoundingRect()` | → `{ x, y, width, height, top, right, bottom, left }` / `null` |

**Elemen tidak ditemukan** tidak pernah melempar untuk `click/fill` — kembalikan
`{ success:false, error }`. Hanya `waitFor`/`waitForSelector`/`goto`/`waitForLoad` yang
melempar `Error` (bisa `try/catch`).

```javascript
var email = page.locator('input[name="email"]');
if (email.exists()) email.fill("user@example.com");
var rows = page.locator("table tbody tr").all();
```

---

## 6. Skrip Contoh Lengkap

Lihat `capcut_test.js` (root repo) — generator akun CapCut:

```javascript
function createCapcutAccount(email) {
    var browser = RoCatBrowser.launch({ headless: false });
    var page = browser.newPage();
    page.goto("https://www.capcut.com/signup", { waitUntil: "domcontentloaded", timeout: 30000 });
    page.waitForTimeout(2000);

    page.click('button:has-text("Continue with email")');
    page.waitForTimeout(1000);

    var emailInput = page.locator('input[type="email"], input[name="username"]');
    emailInput.waitFor(5000);
    emailInput.fill(email);
    page.click('button:has-text("Continue")');
    page.waitForTimeout(1500);

    page.locator('input[type="password"]').waitFor(5000);
    page.locator('input[type="password"]').fill(password);
    page.click('button:has-text("Sign up"), button:has-text("Register")');

    page.locator('input[placeholder="Year"]').fill(String(birthday.year));
    page.click('select[name="month"]'); page.click('option:has-text("March")');
    page.click('select[name="day"]');   page.click('option:has-text("14")');
    page.click('button:has-text("Next"), button:has-text("Submit")');

    var shot = page.screenshot({ quality: 80 });
    browser.close();
    return { email: email, password: password, screenshot: shot, status: "PENDING_OTP" };
}
```

---

## 7. Panduan Migrasi dari Playwright / Puppeteer

| Playwright / Puppeteer | RoCatBrowser |
|------------------------|--------------|
| `await chromium.launch({headless})` | `RoCatBrowser.launch({headless})` |
| `await browser.newPage()` | `browser.newPage()` |
| `await page.goto(url, {waitUntil})` | `page.goto(url, {waitUntil})` |
| `await page.locator(sel).fill(t)` | `page.locator(sel).fill(t)` |
| `await page.click(sel)` | `page.click(sel)` |
| `await page.waitForSelector(sel, {timeout})` | `page.waitForSelector(sel, timeoutMs)` |
| `await page.evaluate(fn, arg)` | `page.evaluate(fn, [arg])` |
| `await page.title()` / `.url()` | `page.title()` / `.url()` |
| `await page.screenshot({path})` | `page.screenshot({path})` |
| `await page.cookies()` | `page.cookies()` |
| `await page.waitForTimeout(ms)` | `page.waitForTimeout(ms)` |

**Perbedaan kunci:**
- Semua panggilan **sinkron** — tidak ada `await`. Return hasil berupa nilai langsung.
- Selector **hanya CSS** (tidak ada `text=…`, `:has-text`, `getByRole`, dsb. milik Playwright).
- `click`/`fill` mengembalikan `{ success, error }`, bukan `undefined`.
- `waitForSelector` menerima `timeoutMs` angka, bukan objek `{ timeout }`.
- `evaluate` menerima argumen sebagai **array**, bukan variadic.
- Timeout default global dapat diubah `RoCatBrowser.setDefaultTimeout(ms)`.

---

## 8. Implementasi & Arsitektur

```
┌─────────────────────────────────────────────────────────────┐
│                    RoCat App (Existing)                     │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────────────────────┐ │
│  │  Script Canvas   │    │  WebView (Hidden, Headless)    │ │
│  │  (Rhino Engine)  │◄──►│  RoCatBrowserBridge.kt         │ │
│  │                  │    │  HeadlessWebViewManager.kt     │ │
│  └─────────────────┘    └─────────────────────────────────┘ │
│           │                         │                        │
│           ▼                         ▼                        │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ RoCatBrowser (JS polyfill, RoCatBrowserWrapper.kt)     ││
│  │  Browser / Page / Locator — sinkron, Playwright-like   ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

| File | Peran |
|------|-------|
| `scripting/api/.../ScriptBrowserBridge.kt` | Kontrak bridge + perintah general-purpose (default no-op) |
| `scripting/rhino/.../RoCatPageBridge.kt` | Global `RoCatPage` (low-level, primitif) |
| `scripting/rhino/.../RoCatBrowserWrapper.kt` | Polyfill `RoCatBrowser` (Browser/Page/Locator) |
| `scripting/rhino/.../RhinoScriptEngine.kt` | Inject `RoCatPage` + `RoCatBrowser` ke scope |
| `app/.../scripting/RoCatBrowserBridge.kt` | Implementasi app dari `ScriptBrowserBridge` |
| `app/.../scripting/HeadlessWebViewManager.kt` | Manajer WebView tersembunyi (main-thread marshalling) |

### Keputusan desain

| Keputusan | Alasan |
|-----------|--------|
| Tanpa modifikasi aplikasi inti | Skrip berjalan di atas WebView yang sudah ada |
| API sinkron | Rhino 1.7.15 tidak punya `async/await` |
| Polyfill Playwright | API familier, tapi internal sync |
| Satu WebView, banyak "page" | Navigasi bergantian; hemat memori |
| Cookie via `CookieManager` | Store yang sama dengan OkHttp (`AndroidCookieJar`) |

---

## 9. Batasan

- **Satu** WebView per aplikasi; `newPage()` tidak membuka tab terpisah — semua page
  berbagi WebView yang sama (halaman aktif terakhir menang).
- Selector **CSS saja** (tanpa pseudo Playwright `text=`, `:has-text`, dsb).
- `evaluate` hasil harus JSON-serializable (WebView `evaluateJavascript` contract).
- `type(text, delay)` memakai per-karakter `fill` → lambat untuk teks panjang.
- Screenshot memakai viewport default 1366×768 (WebView tersembunyi).
- Instruksi watchdog Rhino (10M) tetap berlaku — hindari loop tak berujung.
- Panggil `browser.close()` / `page.close()` di blok `finally` agar WebView tidak bocor.