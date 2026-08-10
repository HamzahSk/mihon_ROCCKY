# Role and Objective
Kamu adalah **Senior Android Engineer, Compiler Architect, dan Technical Writer** handal. Kita sekarang masuk ke **Tahap 23: Implementasi Dual-Mode Scraping Engine (Standard Fetch + Puppeteer-like Headless WebView), Pembaruan Dokumentasi**.

Fokus tahap ini adalah membangun kapabilitas arsitektur *dual-mode* pada *engine* RoCat, sehingga skrip dapat berjalan dengan dua metode yang fleksibel:
1. **Mode Statis / Normal (Standard Scrape):** Menggunakan `fetch()` (OkHttp) + `RoCatDOM` (Jsoup) untuk ekstraksi cepat, ringan, dan hemat daya.
2. **Mode Interaktif / Headless Automation (Puppeteer-like):** Menggunakan Android WebView Bridge (`RoCatPage` / `RoCatBrowser`) yang mensimulasikan interaksi web asli (mengisi form/`type`, mengeklik tombol/`click`, menunggu elemen/`waitForSelector`, eksekusi JS) di *background*, sambil tetap menyajikan UI kustom yang cantik di Canvas RoCat (`RoCatUI`).

---

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan teknis rinci di `ai_memory/task_YYYYMMDD_HHMM_tahap23_dual_mode_engine.md` setelah tahap ini selesai.
2. **Context Path:**
   - File dokumentasi `DOCS_SCRIPTING.md` berada di *root* proyek `rocat-app/`.
   - File skrip masukan `testscrape.txt` berada di *root* atau folder skrip.
   - *Engine* scripting berada di module `scripting/rhino` dan `scripting/api`.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 23.1: Implementasi Engine Native (`ScriptBrowserBridge` & `HeadlessWebViewManager`)
- Buat komponen Kotlin baru di layer Android / Scripting Bridge:
  - `HeadlessWebViewManager.kt`: Mengelola instance `WebView` tersembunyi (*headless*) di *main thread* secara aman, menangani pemuatan URL, eksekusi JavaScript, dan *event listener*.
  - `ScriptBrowserBridge.kt` / `RoCatPageBridge.kt`: Mengisi jembatan (*bridge*) Java/Kotlin ke mesin JS Rhino, mengekspos objek global `RoCatPage` (atau `RoCatBrowser`).
- **Spesifikasi API `RoCatPage` yang Wajib Didefinisikan:**
  - `RoCatPage.open(url, timeoutMs)`: Membuka URL di Headless WebView dan menunggu hingga *page loaded*.
  - `RoCatPage.type(selector, text)`: Mengisi kolom input pada *selector* CSS tertentu.
  - `RoCatPage.click(selector)`: Memicu *click event* pada elemen target di WebView.
  - `RoCatPage.waitForSelector(selector, timeoutMs)`: Menunggu hingga elemen CSS tertentu muncul di DOM.
  - `RoCatPage.evaluate(scriptJs)`: Mengeksekusi JS kustom di dalam konteks web asli dan mengembalikan hasilnya ke Rhino.
  - `RoCatPage.getHtml()`: Mengambil string HTML yang telah di-render secara dinamis oleh WebView.
  - `RoCatPage.close()`: Membersihkan memori dan melepaskan instance WebView.
- **Mekanisme Blocking & Synchronization:** Karena Rhino berjalan di *background thread* dan WebView wajib di-akses di *Main Thread* (UI Thread), gunakan `CountDownLatch` atau *suspend/runBlocking bridge* agar fungsi `RoCatPage` bersifat **sinkron** bagi skrip JS, tanpa membekukan UI utama aplikasi.

### Tahap 23.2: Integrasi Dual-Mode ke Rhino Environment
- Daftarkan `RoCatPage` ke dalam `RhinoScriptEngine` bersama dengan `RoCatUI`, `RoCatDOM`, `fetch`, dan `RoCat`.
- Pastikan skrip dapat memadukan (*hybrid*) kedua mode secara bebas dalam satu alur eksekusi:
  - Contoh: Menggunakan `fetch()` normal untuk memuat daftar anime, tetapi beralih menggunakan `RoCatPage` ketika perlu menembus *form login*, *anti-bot challenge*, atau *player iframe* yang di-generate via JavaScript rumit.

### Tahap 23.3: Perbaikan Skrip `testscrape.txt` → `fixed_testscrape.js`
- Analisis struktur dan kegagalan pada skrip `testscrape.txt`.
- Tulis ulang skrip menjadi `fixed_testscrape.js` dengan menerapkan API terbaru:
  - Gunakan `RoCat.render()` untuk manajemen UI Canvas.
  - Gunakan `RoCat.safeParseJson()` dan `RoCat.fetchJson()` untuk *error handling* tangguh.
  - Tunjukkan demonstrasi *dual-mode*: gunakan `fetch` + `RoCatDOM` untuk pencarian/home, dan `RoCatPage` jika memerlukan simulasi interaksi form/button di web target.
  - Tampilkan status dan log menggunakan `RoCatUI.addAlert()`, `RoCatUI.addBadgeGroup()`, dan `RoCatUI.addJsonLog()`.

### Tahap 23.4: Pembaruan Dokumentasi `DOCS_SCRIPTING.md`
- Edit file `DOCS_SCRIPTING.md` di *root* proyek:
  1. **Bab Arsitektur Dual-Mode Scraping:**
     - Jelaskan perbedaan **Mode Statis** (HTTP Fetch + Jsoup) vs **Mode Interaktif** (Headless WebView `RoCatPage`).
     - Panduan kapan harus menggunakan masing-masing mode (efisiensi vs kapabilitas interaktif).
  2. **Dokumentasi Lengkap API `RoCatPage`:**
     - Parameter, nilai kembalian, dan contoh penggunaan `open`, `type`, `click`, `waitForSelector`, `evaluate`, `getHtml`, `close`.
  3. **Dokumentasi Komponen UI & Helper Tahap 22/23:**
     - `RoCat.render(items)`
     - `RoCat.safeParseJson(str, fallback)` & `RoCat.fetchJson(url, options)`
     - `RoCatUI.addJsonLog()`, `addHtmlPreview()`, `addAudio()`, `addAlert()`, `addBadgeGroup()`
  4. **Boilerplate Skrip Hybrid Baru:**
     - Sediakan contoh skrip lengkap yang mendemonstrasikan perpaduan Mode Statis & Mode Interaktif.

### Tahap 23.5: Testing & Update Memory
- Jalankan/buat unit test di `scripting/rhino` untuk memverifikasi eksekusi `RoCatPage` dan komponen UI baru.
- Perbarui `ai_memory/00_INDEX.md` dengan menandai Tahap 23 **SELESAI**.
- Buat catatan teknis di `ai_memory/task_YYYYMMDD_HHMM_tahap23_dual_mode_engine.md` yang merangkum arsitektur native bridge WebView, perubahan pada Rhino runner, dan perbaikan skrip `fixed_testscrape.js`.