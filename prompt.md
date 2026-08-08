# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Tahap 9 (Fix Rhino Class Loader & Playground UI) telah berhasil diselesaikan.

Tugasmu di **Tahap 10** adalah melakukan perombakan tingkat lanjut pada sistem *Networking* dan *Cookie Management*. Aplikasi saat ini masih bertingkah seperti `fetch` API biasa. Agar bisa mengeksekusi skrip *scraper* pada web modern (terutama yang dilindungi Cloudflare), aplikasi harus bertingkah selayaknya **Stealth Browser (Chromium-like)**. Kamu harus mengimplementasikan sinkronisasi *Cookie* tingkat browser dan interceptor khusus untuk menembus *Cloudflare JS Challenge*.

**PENTING (REFERENSI MIHON):** Gunakan arsitektur Mihon (direktori root) sebagai acuan untuk membuat `AndroidCookieJar` (sinkronisasi WebView CookieManager) dan `CloudflareInterceptor`.

---

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md`.
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan tugas terperinci di `ai_memory/task_YYYYMMDD_HHMM_tahap10_stealth_browser_and_cloudflare.md` setelah tahap ini selesai.
2. **Kepatuhan Stealth Network:**
   - Permintaan HTTP via OkHttp dan WebView harus berbagi *Cookie* yang sama persis (persisten).
   - `User-Agent` harus sinkron dan terlihat seperti browser desktop/mobile asli.
3. **Build Verification:**
   - Pastikan kode dapat dikompilasi sukses dengan `./gradlew assembleDebug`.

---

# Bug & Issue Analysis
1. **Masalah Cookie Murni:** OkHttp saat ini tidak menyimpan *cookie* secara persisten atau membagikannya dengan WebView. Login *session* atau *token* yang didapat dari satu request hilang di request berikutnya.
2. **Cloudflare / Anti-Bot Block:** Saat melakukan `fetch` ke situs ber-Cloudflare, server merespons dengan HTTP 403 atau 503 (Cloudflare Challenge) karena mendeteksi request tersebut bukan dari browser asli. Aplikasi tidak memiliki mekanisme untuk menyelesaikan *challenge* tersebut.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 10.1: Implementasi Android WebView CookieJar
1. **Buat `AndroidCookieJar`:**
   - Buat class yang mengimplementasikan `CookieJar` dari OkHttp.
   - Di dalam fungsi `saveFromResponse` dan `loadForRequest`, gunakan `android.webkit.CookieManager.getInstance()` untuk menyimpan dan membaca *cookie*.
   - Pastikan pemanggilan `CookieManager` aman dari *thread* lain (gunakan `CookieManager.getInstance().setCookie()` dan `getCookie()`).
2. **Integrasi ke `NetworkHelper`:**
   - Tambahkan `AndroidCookieJar` ini ke dalam konfigurasi `OkHttpClient.Builder` di `NetworkHelper`.

### Tahap 10.2: Implementasi Cloudflare Interceptor (Mihon Style)
1. **Buat `CloudflareInterceptor`:**
   - Buat `Interceptor` OkHttp yang mencegat respons (response).
   - Deteksi apakah respons tersebut merupakan *Cloudflare Challenge* (biasanya HTTP 503 atau 403, dan header `Server: cloudflare`, atau ada teks `<title>Just a moment...</title>` di body).
2. **Mekanisme Bypass (Hidden WebView):**
   - Jika terdeteksi CF, jalankan sebuah *headless* `WebView` (atau WebView transparan di thread utama) yang memuat URL tersebut dengan User-Agent yang sama persis.
   - Karena WebView adalah browser sungguhan, ia akan mengeksekusi JS Turnstile/Cloudflare.
   - Pantau perubahan status (via `WebViewClient.onPageFinished` atau injeksi JS) hingga *challenge* selesai dan *cookie* `cf_clearance` berhasil didapatkan oleh `CookieManager`.
   - Setelah *cookie* tersimpan, ulangi request OkHttp asli (`chain.proceed(request)`) yang kini akan otomatis membawa `cf_clearance` dari `AndroidCookieJar`.

### Tahap 10.3: Penyesuaian Fetch Bridge & Headers
1. **Stealth Headers:**
   - Pastikan `UserAgentInterceptor` sudah bekerja.
   - Tambahkan *default headers* standar browser modern (seperti `Accept-Language`, `Sec-Ch-Ua`, `Sec-Fetch-Dest`, dsb.) ke dalam request jika belum ada, agar semakin sulit dideteksi sebagai *bot*.
2. **Sinkronisasi Script API:**
   - Pastikan API `fetch` di JavaScript tetap sinkron dan sekarang mendapatkan manfaat langsung dari *CookieJar* dan *Interceptor* ini tanpa perlu mengubah kode JS.

### Tahap 10.4: Verifikasi & Pembaruan Memori
1. **Build & Test:**
   - Jalankan `./gradlew :app:assembleDebug`.
   - Pastikan tidak ada *memory leak* pada penggunaan WebView di Interceptor.
2. **Pembaruan Memori:**
   - Catat detail arsitektur `AndroidCookieJar` dan `CloudflareInterceptor` ke `ai_memory/task_YYYYMMDD_HHMM_tahap10_stealth_browser_and_cloudflare.md`.
   - Perbarui status di `ai_memory/00_INDEX.md` menjadi **Tahap 10 SELESAI**.

---

**Instruksi Eksekusi:**
Konfirmasi bahwa kamu membaca `memory_prompt.md`. Mulai dari **Tahap 10.1** untuk membuat `CookieJar` berbasis WebView, lalu lanjutkan merancang `CloudflareInterceptor` di **Tahap 10.2**. Uji kompilasi dengan `./gradlew assembleDebug`, dan perbarui riwayat memori setelah selesai.
