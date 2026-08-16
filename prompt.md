# Role and Objective
Kamu adalah **Senior Android Engineer, Automation Expert, dan Architect** handal. Objektif utama kita pada tahap ini adalah membawa kemampuan otomatisasi *headless browser* ala Playwright/Puppeteer ke dalam ekosistem RoCat, sehingga kita bisa menjalankan skrip pendaftaran akun CapCut secara otomatis.

Saat ini, mesin eksekusi RoCat (Rhino 1.7.15) hanya mendukung request HTTP sinkron dan tidak mendukung `async/await` atau interaksi DOM secara langsung[span_0](start_span)[span_0](end_span). Oleh karena itu, tugas utamamu adalah:
1. **Memodifikasi Arsitektur Core RoCat:** Membangun atau mengintegrasikan *engine* baru (misalnya menggunakan WebView terkontrol, GeckoView, atau JavaScriptCore/V8) agar aplikasi Android RoCat dapat menjalankan *script* asinkron dan berinteraksi dengan elemen web (mengisi form, klik tombol).
2. **Membuat Script CapCut Generator:** Menulis ulang logika skrip Playwright (CapCut Account Generator) menjadi format RoCat yang baru, menyimpannya di file `capcut_test.js`, dan mengujinya.

---

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan teknis terkait perombakan arsitektur di `ai_memory/task_YYYYMMDD_HHMM_browser_automation_engine.md`.
2. **Context Path:**
   - Script Engine saat ini berada di `scripting/rhino/.../RhinoScriptEngine.kt`. Kamu perlu membuat *bridge* baru untuk mendukung eksekusi asinkron dan manipulasi DOM.
   - Skrip pengujian harus disimpan tepat di file `capcut_test.js`.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 1: Arsitektur Eksekusi Asinkron & Browser Control
Karena skrip generator akun sangat bergantung pada interaksi dinamis dan *delay* (menunggu elemen muncul), *engine* HTTP sinkron saat ini tidak memadai.
- Integrasikan mesin JavaScript yang mendukung `async/await` (seperti QuickJS atau integrasikan *bridge* yang dalam ke Android WebView).
- Buat API Jembatan baru (misalnya `RoCatBrowser`) yang memungkinkan skrip untuk:
  - Membuka URL di WebView *headless* atau *visible* di latar belakang.
  - Melakukan injeksi JavaScript untuk mencari elemen (mirip `page.locator`).
  - Menyimulasikan aksi klik (`click()`) dan ketik (`fill()`) pada elemen DOM.
  - Menunggu (*wait*) elemen dirender atau melakukan *timeout*.

### Tahap 2: API Pembuatan Data Acak di Skrip
Buat utilitas bawaan atau pastikan mesin JS yang baru mendukung penuh fungsi pembuatan data.
- Pastikan logika pembuatan *password* acak dan tanggal lahir dapat berjalan mulus di *engine* baru.

### Tahap 3: Implementasi Script Generator di `capcut_test.js`
Tulis ulang skrip Node.js/Playwright berikut menggunakan API `RoCatBrowser` yang baru saja kamu bangun. **Simpan hasil konversi skrip ini ke dalam file bernama `capcut_test.js`**. Skrip ini harus mendaftarkan akun non-pro.

**Referensi Logika Skrip:**
- Buka URL: `https://www.capcut.com/signup`
- Klik opsi "Continue with email".
- Tunggu input email muncul, lalu isi dengan email yang di-generate.
- Klik "Continue".
- Isi input *password* dengan *password* acak.
- Klik tombol "Sign up / Register".
- Isi form tanggal lahir (Tahun, Bulan, Hari) menggunakan elemen *dropdown*/*combobox*.
- Klik "Submit" / "Next".
- Tampilkan pesan ke antarmuka log untuk menunggu OTP secara manual (skrip di-*pause* sementara menunggu user).

### Tahap 4: Bypass & Stealth Mechanisms
- Pastikan `RoCatBrowser` mewarisi atau mengadaptasi *Cloudflare Bypasser* dan injeksi HTTP *Header* rahasia (seperti `Accept-Language`, `User-Agent`) yang sudah ada di RoCat[span_1](start_span)[span_1](end_span) agar CapCut tidak mendeteksi sesi sebagai bot.
- Terapkan mekanisme mematikan fitur deteksi otomatisasi.

### Tahap 5: Testing (`capcut_test.js`) & Update Memory
- Verifikasi bahwa aplikasi Android bisa mengompilasi *engine* baru ini tanpa konflik.
- **Lakukan uji coba eksekusi langsung menggunakan file `capcut_test.js`** untuk memastikan aplikasi tidak *crash* atau *freeze* (ANR) saat menunggu elemen DOM atau OTP.
- Perbarui `ai_memory/00_INDEX.md` dengan status penyelesaian.
