
# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Kita akan melanjutkan pengembangan aplikasi ke **Tahap 12: Media Renderer & Smart Playground Inputs**.
Tujuan utama tahap ini adalah membuat Playground bisa me-render *output* gambar/video, menyederhanakan *form* input parameter, dan memfilter fungsi mana yang boleh dieksekusi oleh pengguna (Public vs Private).

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md`.
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan tugas terperinci di `ai_memory/task_YYYYMMDD_HHMM_tahap12_media_renderer_and_smart_inputs.md` setelah tahap ini selesai.
2. **Build Verification:**
   - Pastikan setiap perubahan dikonfirmasi dengan `./gradlew assembleDebug` tanpa *error* kompilasi sebelum kamu menyatakan tugas selesai.

---

# Feature Requirements Analysis
1. **Media Preview (Image & Video) di Result Card:**
   - Jika skrip menghasilkan *output* JSON dengan format kontrak tertentu (misalnya memiliki properti `media_type: "image" | "video"` dan `media_url: "..."`), UI Playground tidak hanya menampilkan teks JSON-nya, tetapi juga merender medianya di atas log JSON.
   - Gunakan pustaka **Coil** (`AsyncImage`) di Jetpack Compose untuk memuat gambar dari URL.
   - Untuk Video, gunakan *fallback* sederhana seperti tombol "Play Video" yang memicu `Intent.ACTION_VIEW` ke aplikasi pemutar video bawaan / *browser* menggunakan URL tersebut.
2. **Simplified Dynamic Inputs (Value Only):**
   - Hapus konsep "Key" pada input. Saat memanggil fungsi JS, pengguna hanya perlu memasukkan nilainya (*value*) secara berurutan (Arg 1, Arg 2, dst).
   - Secara *default*, sediakan 1 baris input (*value* saja) kosong jika daftar input belum ada. Jika *user* butuh 2 argumen (misal: URL dan Format), mereka tinggal menekan "+ Add Input" untuk menambah baris *value* kedua.
3. **Public vs Private Functions (Visibility):**
   - Skrip yang kompleks pasti punya banyak fungsi pembantu (*helper*). Fungsi ini tidak boleh dieksekusi langsung dari Playground.
   - **Aturan Konvensi:** Fungsi yang namanya diawali dengan *underscore* (contoh: `_fetchData`, `_parseHTML`) dianggap sebagai **Private Function**.
   - Fungsi normal (contoh: `main`, `getDetail`) dianggap sebagai **Public Function**.
   - *Dropdown Selector* hanya boleh menampilkan *Public Functions*.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 12.1: Update `PlaygroundViewModel.kt` & Regex Parser
- Ubah state argumen dari `List<Pair<String, String>>` atau data class *Key-Value* menjadi sekadar `List<String>`. 
- Perbarui logika Regex di `extractFunctionNames`. Gunakan *negative lookahead* agar fungsi yang diawali dengan `_` (underscore) diabaikan.
  - Contoh Regex: `function\s+(?!_)([a-zA-Z$][\w$]*)\s*\(`
- Sesuaikan fungsi eksekusi `runFunction()` agar hanya meneruskan daftar nilai *string* (arguments) tersebut ke `ExecuteScript.invoke()`.

### Tahap 12.2: Update `PlaygroundScreen.kt` (Input UI)
- Rombak bagian input parameter: Hilangkan kolom "Key". Cukup gunakan satu `OutlinedTextField` *full-width* dengan label "Argument Value (e.g. URL)" untuk setiap item di dalam *list* argumen.
- Tombol hapus (ikon *Delete*) tetap ada di sebelah kanan tiap input.
- Default state: tampilkan 1 kolom input kosong.

### Tahap 12.3: Update UI Result / Media Renderer
- Buat komponen `MediaPreviewRenderer` di dalam atau di atas `CopyableResultCard`.
- Lakukan *parsing* hasil JSON *output* secara dinamis. Jika mengandung `"media_type": "image"` dan `"media_url"`, tampilkan `AsyncImage` (Coil) menggunakan URL tersebut dengan `contentScale = ContentScale.Fit` dan batas tinggi wajar (misal `heightIn(max = 300.dp)`).
- Jika `"media_type": "video"`, tampilkan sebuah `OutlinedButton` atau `ElevatedCard` berisi ikon *Play* yang jika diklik akan memicu `Intent(Intent.ACTION_VIEW, Uri.parse(media_url))` dengan MIME type `video/*`.

### Tahap 12.4: Verifikasi & Pembaruan Memori
- Gunakan dan tes dengan format *script* berikut di Playground:

```javascript
// ==UserScript==
// @name        Media Downloader Tester
// @version     1.0.0
// @description Test Image and Video output with private functions
// @author      Tester
// @match       *://*/*
// ==/UserScript==

// Fungsi ini PRIVATE (tidak akan muncul di dropdown)
function _getResolution(url) {
    return "1080p"; // Simulasi proses internal
}

// Fungsi PUBLIC 1: Hanya butuh 1 argumen (URL)
function getImage(url) {
    return {
        media_type: "image",
        media_url: url,
        title: "Test Image",
        resolution: _getResolution(url)
    };
}

// Fungsi PUBLIC 2: Butuh 2 argumen (URL, Format)
function getVideo(url, format) {
    return {
        media_type: "video",
        media_url: url,
        format_requested: format,
        resolution: _getResolution(url)
    };
}

```
 * Uji cobakan: masukkan URL gambar valid (.jpg/.png) ke dalam *Argumen 1* dan panggil fungsi getImage. Pastikan gambar ter-render di UI di atas log JSON.
 * Jalankan ./gradlew :app:assembleDebug.
 * Perbarui status di ai_memory/00_INDEX.md menjadi **Tahap 12 SELESAI** dan buat catatan di file memori baru sesuai instruksi (CRITICAL).