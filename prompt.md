# Role and Objective
Kamu adalah AI Software Engineer dan Technical Writer handal. Kita sekarang masuk ke **Tahap 21: Pembuatan Dokumentasi Pembuatan Skrip RoCat (Rocat Scripting API Docs)**.
Fokus tahap ini adalah menyusun panduan lengkap (dokumentasi) bagi developer/kreator skrip tentang cara membuat skrip *scraper* untuk aplikasi RoCat. 

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan di `ai_memory/task_YYYYMMDD_HHMM_tahap21_scripting_docs.md` setelah tahap ini selesai.
2. **Context Path:**
   - Buat file dokumentasi baru di *root* proyek `rocat-app/` dengan nama `DOCS_SCRIPTING.md`.
3. **Akurasi Data:**
   - Pastikan dokumentasi **100% akurat** dengan API yang sudah kita bangun di tahap-tahap sebelumnya (terutama `ScriptUiBridge`, `JsoupBridge`, `NetworkHelper`/`fetch`, dan `Media3`/ExoPlayer integration).

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 21.1: Struktur Dasar Skrip & Metadata
- Buat file `DOCS_SCRIPTING.md`.
- Jelaskan blok metadata wajib di awal skrip (format `==UserScript==` atau `// @tag`).
- Daftarkan *tag* yang didukung (misalnya `@name`, `@version`, `@description`, `@author`, `@match`, `@icon`, `@category`).
- Jelaskan siklus hidup skrip (Script Lifecycle), terutama fungsi `onLaunch()` yang dipanggil otomatis saat skrip pertama kali dijalankan di kanvas.

### Tahap 21.2: Dokumentasi UI Bridge (`RoCatUI`)
Jelaskan secara detail semua metode yang tersedia di objek global `RoCatUI`, beserta parameter dan contoh penggunaannya:
- **Input & Interaksi:** `addInput(id, hint)`, `addButton(label, functionName)`
- **Media & Pratinjau:** - `addImage(url, title, allowDownload)`
  - `addVideo(url, title, isStreamHls, allowDownload)` -> Jelaskan bahwa HLS stream otomatis menggunakan pemutar ExoPlayer native.
  - `thumbnailPreview(url)` dan `videoPreview(url)` (sebagai referensi backward-compatibility).
- **Layouting:** `addGrid(columns, itemsJson, onClickFunction)` -> Jelaskan format JSON yang dibutuhkan.
- **Utility UI:** `clear()`, `log(text)`

### Tahap 21.3: Dokumentasi DOM Parsing (`RoCatDOM`)
Jelaskan objek global `RoCatDOM` yang menggantikan Cheerio/Jsoup murni, beserta metodenya:
- Cara *parsing* HTML statis: `parse(html)`
- Metode penelusuran DOM: `select()`, `selectText()`, `selectAttr()`, `selectHtml()`
- Metode elemen (*wrapper*): `text()`, `html()`, `attr()`, `find()`, dll.
- Berikan contoh singkat cara mengekstrak daftar episode atau judul dari HTML Anichin/Manga.

### Tahap 21.4: Dokumentasi Network & Utilities
- **Fetch API:** Jelaskan penggunaan fungsi `fetch(url, options)` *synchronous* bawaan RoCat. Ingatkan bahwa eksekusi Rhino di Android tidak mendukung `async/await`, jadi *fetch* bekerja secara sinkron.
- **Stealth & Interceptor:** Beri tahu bahwa *fetch* otomatis diproteksi oleh *Cloudflare Bypasser*, *Custom User-Agent*, dan *Custom DNS* bawaan aplikasi.
- **Native Utilities:** Jelaskan penggunaan fungsi seperti `RoCatUI.decodeBase64(str)` dan `RoCatUI.save(fileName, content, mimeType)`.

### Tahap 21.5: Contoh Skrip Lengkap (Boilerplate)
- Sediakan satu contoh kode skrip *scraper* (Boilerplate) fiktif yang menggabungkan Metadata, `onLaunch`, `RoCatUI.addGrid`, pengambilan *fetch*, dan pemanggilan video HLS.

### Tahap 21.6: Update Memory
- Perbarui file `00_INDEX.md` dengan status **Tahap 21 SELESAI**.
- Buat catatan teknis di `task_YYYYMMDD_HHMM_tahap21_scripting_docs.md`.
