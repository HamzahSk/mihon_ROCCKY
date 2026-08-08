# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Tahap 7 (Perbaikan Network, SSL, dan Injeksi WebView) telah selesai diimplementasikan.

Tugasmu di **Tahap 8** adalah membangun **Script Execution API, Native DOM Bridge (Jsoup), dan Testing UI**. Aplikasi harus bisa mengeksekusi fungsi spesifik di dalam skrip (seperti `search` atau `detail`) dengan menerima input dinamis langsung dari tombol dan *text field* di UI. Selain itu, kamu harus menyediakan fungsi *parsing* HTML bawaan agar skrip tidak bergantung pada *library* Node.js eksternal seperti `cheerio`.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md`.
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan tugas terperinci di `ai_memory/task_YYYYMMDD_HHMM_tahap8_script_api_and_ui_execution.md` setelah tahap ini selesai.
2. **Keterbatasan Rhino Engine (Sangat Penting):**
   - Berdasarkan `00_INDEX.md`, Rhino 1.7.15 **TIDAK MENDUKUNG `async/await`**. Oleh karena itu, skrip contoh yang menggunakan `async/await` dan `import` harus diubah menjadi **synchronous JavaScript**.
   - Fungsi `fetch` di aplikasi ini sudah dikonfigurasi untuk berjalan secara *synchronous* mengembalikan objek *Response*.
3. **Build Verification:**
   - Pastikan kode dapat dikompilasi sukses dengan `./gradlew assembleDebug`.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 8.1: Implementasi Native DOM Bridge (Pengganti Cheerio)
1. **Integrasi Jsoup:**
   - Tambahkan implementasi `org.jsoup:jsoup` di `build.gradle` (jika belum ada).
2. **Buat `JsoupBridge`:**
   - Buat class/objek Kotlin bernama `JsoupBridge` yang mengekspos fungsi-fungsi dasar parsing HTML untuk JavaScript.
   - Contoh fungsi yang bisa dipanggil dari JS:
     - `parse(html: String)`: Mengembalikan representasi *Document* Jsoup.
     - `selectText(html: String, selector: String)`: Mengembalikan teks dari elemen yang dipilih.
     - `selectAttr(html: String, selector: String, attr: String)`: Mengembalikan atribut tertentu.
3. **Injeksi ke JS Engine:**
   - Daftarkan `JsoupBridge` ke dalam `ScriptContext` Rhino dengan nama global (misal: `RoCatDOM`), sehingga skrip bisa langsung memanggil `RoCatDOM.selectText(html, ".title")`.

### Tahap 8.2: Penambahan UI (Input & Tombol Eksekusi)
1. **Modifikasi `PlaygroundScreen` atau `ScriptDetailScreen`:**
   - Tambahkan *Section* khusus bernama **"Test Execution"**.
   - Tambahkan satu `OutlinedTextField` untuk **Input Parameter** (bisa berupa teks *query* pencarian atau URL detail).
   - Tambahkan **Dua Buah Tombol (Button)**:
     - **Tombol "Run Search"**: Saat diklik, akan memanggil fungsi `search(query)` di dalam skrip.
     - **Tombol "Run Detail"**: Saat diklik, akan memanggil fungsi `detail(url)` di dalam skrip.
   - Tambahkan satu komponen *Text/Log Area* yang *scrollable* untuk menampilkan hasil JSON balikan dari skrip tersebut.

### Tahap 8.3: Integrasi Eksekusi di ViewModel
1. **Gunakan `Invocable` Rhino:**
   - Di dalam *ViewModel* terkait (saat tombol diklik), eksekusi *source code* skrip terlebih dahulu untuk mendaftarkan fungsi-fungsinya ke konteks.
   - Gunakan `(engine as Invocable).invokeFunction("search", queryInput)` atau `invokeFunction("detail", urlInput)` untuk memicu spesifik fungsi di JS.
   - Tangkap balikan objek dari JS, konversi ke *String* (menggunakan `JSON.stringify` atau konverter bawaan), dan tampilkan ke *Log Area* UI di *main thread*.
   - **Ingat:** Pastikan eksekusi pemanggilan fungsi ini dibungkus dalam `viewModelScope.launch(Dispatchers.IO)` karena ada proses *network* di dalamnya.

### Tahap 8.4: Pembuatan Script Template Synchronous
1. **Konversi Script Contoh (MangaUpdates):**
   - Tulis ulang logika `mangaupdate.js` menjadi versi *synchronous* yang murni kompatibel dengan Rhino di Android.
   - Hilangkan `import * as cheerio`.
   - Ubah `async function search()` menjadi `function search(query)`.
   - Ubah `await fetch(...)` menjadi pemanggilan sinkron: `var res = fetch(...); var html = res.text();`
   - Ganti implementasi parser dari Cheerio menjadi pemanggilan `RoCatDOM`.
   - Letakkan kode ini di fitur "Load example" pada UI "Add Script".

### Tahap 8.5: Verifikasi & Pembaruan Memori
1. **Testing:**
   - Jalankan `./gradlew :app:assembleDebug`.
   - Buka aplikasi, masukkan *script* hasil konversi ke *Playground*. Ketik nama komik di kolom input, lalu klik tombol **"Run Search"**. Pastikan hasilnya muncul di *Log Area* UI.
2. **Pembaruan Memori:**
   - Catat detail arsitektur *Jsoup bridge*, modifikasi tombol UI, dan format skrip standar ke `ai_memory/task_YYYYMMDD_HHMM_tahap8_script_api_and_ui_execution.md`.
   - Perbarui status di `ai_memory/00_INDEX.md` menjadi **Tahap 8 SELESAI**.

---

**Instruksi Eksekusi:**
Konfirmasi bahwa kamu membaca `memory_prompt.md`. Mulai dari **Tahap 8.1** untuk membuat `JsoupBridge`, lalu lanjutkan ke **Tahap 8.2** untuk menambah *Text Field* dan Tombol eksekusi di UI. Konfirmasi kembali di setiap langkah sebelum melakukan tes build `./gradlew assembleDebug`.
