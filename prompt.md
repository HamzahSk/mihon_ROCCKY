
# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Kita masuk ke **Tahap 13: Full Script-Driven Canvas & Grid System (Mihon-like App Experience)**.
Tujuan tahap ini adalah menyempurnakan UI agar skrip berjalan di layar khusus (*blank canvas*) layaknya membuka ekstensi di Mihon. Skrip tidak lagi dieksekusi di "Playground" yang kaku, melainkan di layar kanvas kosong di mana skrip JS memegang kendali penuh untuk menggambar input, tombol, gambar, grid 3x3, dan me- *redraw* UI untuk berpindah halaman (misal: dari Search ke Detail).

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md`.
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan di `ai_memory/task_YYYYMMDD_HHMM_tahap13_full_script_canvas_and_grid.md` setelah tahap ini selesai.
2. **Build Verification:**
   - Pastikan setiap perubahan dikonfirmasi dengan `./gradlew assembleDebug` tanpa *error* kompilasi.

---

# Feature Requirements Analysis
1. **App Cover & Metadata:**
   - Di `ScriptsScreen` (daftar skrip), gunakan metadata `@icon` atau `@iconURL` dari skrip untuk memuat gambar *cover* (gunakan Coil `AsyncImage`). Jika tidak ada, gunakan ikon *default*.
2. **`ScriptCanvasScreen` (Pengganti Playground):**
   - Saat skrip diklik, jangan buka *PlaygroundScreen* yang lama. Buka `ScriptCanvasScreen` baru.
   - Layar ini benar-benar kosong. Hanya ada `TopAppBar` di bagian atas berisi **Tombol Back** dan **Judul (metadata `@name` skrip)**.
   - Saat layar ini dibuka, otomatis panggil fungsi JS `onLaunch()` untuk menggambar UI awal.
3. **Ekspansi `RoCatUI` (Grid & Navigation):**
   - Tambahkan fungsi `RoCatUI.addGrid(columns, itemsJson, onClickFunctionName)` ke *native bridge*.
     - `columns`: Jumlah kolom (misal: 3 untuk grid 3x3).
     - `itemsJson`: *String* JSON berisi *array of objects* (harus memiliki setidaknya properti `image` dan `title`, serta data *custom* lainnya).
     - `onClickFunctionName`: Nama fungsi JS yang akan dipanggil saat salah satu item di grid diklik, dengan me- *passing* data item tersebut (sebagai JSON string/object).
   - Fungsi `RoCatUI.clear()` akan bertindak sebagai mekanisme "pindah halaman" (menghapus UI saat ini lalu fungsi JS menggambar UI detail).

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 13.1: Persiapan Metadata & List UI
- Update `ScriptsScreen` untuk menampilkan `@icon` / `@iconURL` (jika tersedia di `ScriptMetadata`) di sebelah kiri setiap item list skrip menggunakan Coil.
- Tambahkan rute navigasi baru `ScriptCanvasScreen` di NavGraph, dan arahkan item klik dari `ScriptsScreen` ke layar baru ini.

### Tahap 13.2: Ekspansi `RoCatUI` Bridge & UI State
- Update `ScriptUIComponent` (*sealed class* dari Tahap 12) dengan penambahan:
  ```kotlin
  data class Grid(val columns: Int, val items: List<GridItem>, val onClickFunction: String) : ScriptUIComponent()

```
*(Buat data class GridItem yang berisi properti dasar seperti title, imageUrl, dan rawJsonPayload untuk diteruskan kembali ke JS).*
 * Di UiBridge (RoCatUI), tambahkan fungsi addGrid(columns: Int, itemsJsonString: String, onClickFunction: String). Parse JSON *string* menggunakan org.json.JSONArray atau *kotlinx.serialization* lalu masukkan ke *state*.
### Tahap 13.3: Implementasi ScriptCanvasScreen (Jetpack Compose)
 * Buat Scaffold dengan TopAppBar (tampilkan nama skrip).
 * Bagian konten adalah LazyColumn yang me- *render* ScriptUIComponent.
 * Untuk komponen Grid, gunakan LazyVerticalGrid (dengan perhitungan *height* statis/terukur agar bisa masuk di dalam LazyColumn, atau ubah keseluruhan *layout* menggunakan struktur yang mendukung grid dan list kombinasi).
 * Saat item Grid diklik, panggil ExecuteScript.invoke(..., onClickFunction, listOf(item.rawJsonPayload)).
### Tahap 13.4: Verifikasi dengan Script "Search to Detail"
 * Gunakan skrip JS berikut untuk menguji transisi Search -> Grid -> Detail:
```javascript
// ==UserScript==
// @name        Manga Scraper Mock
// @version     1.0.0
// @icon        [https://via.placeholder.com/150](https://via.placeholder.com/150)
// ==/UserScript==

function onLaunch() {
    RoCatUI.clear();
    RoCatUI.addInput("query", "Cari Manga...");
    RoCatUI.addButton("Search", "doSearch");
}

function doSearch(inputs) {
    var q = inputs.query;
    if (!q) { RoCatUI.log("Masukkan kata kunci!"); return; }
    
    RoCatUI.clear();
    RoCatUI.addButton("Back", "onLaunch");
    RoCatUI.log("Hasil pencarian untuk: " + q);
    
    // Mock Data Grid
    var results = [
        { id: "1", title: "Manga A", image: "[https://via.placeholder.com/300/FF0000](https://via.placeholder.com/300/FF0000)" },
        { id: "2", title: "Manga B", image: "[https://via.placeholder.com/300/00FF00](https://via.placeholder.com/300/00FF00)" },
        { id: "3", title: "Manga C", image: "[https://via.placeholder.com/300/0000FF](https://via.placeholder.com/300/0000FF)" },
        { id: "4", title: "Manga D", image: "[https://via.placeholder.com/300/FFFF00](https://via.placeholder.com/300/FFFF00)" }
    ];
    
    // Tampilkan Grid 3 Kolom
    RoCatUI.addGrid(3, JSON.stringify(results), "openDetail");
}

function openDetail(itemJsonString) {
    var item = JSON.parse(itemJsonString);
    RoCatUI.clear();
    RoCatUI.addButton("Back to Search", "onLaunch"); // Tombol kembali
    RoCatUI.thumbnailPreview(item.image);
    RoCatUI.log("Judul: " + item.title);
    RoCatUI.log("ID Manga: " + item.id);
    RoCatUI.addButton("Baca Chapter 1", "readChapter");
}

function readChapter() {
    RoCatUI.log("Membuka chapter...");
}

```
 * Jalankan ./gradlew :app:assembleDebug.
 * Perbarui status di ai_memory/00_INDEX.md menjadi **Tahap 13 SELESAI** dan catat perubahan arsitektur kanvas ini.
