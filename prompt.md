
# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Kita akan merevisi total arah **Tahap 12: Script-Driven UI & Media Previews (Mihon-Style)**.
Tujuan utamanya adalah mengubah `PlaygroundScreen` agar perilakunya mirip dengan tab ekstensi di Mihon. UI tidak lagi statis atau mengandalkan tebakan output JSON, melainkan **sepenuhnya dikendalikan oleh skrip JS**. Skrip akan menggunakan fungsi *native bridge* untuk menampilkan input, tombol, gambar, atau video.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md`.
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan di `ai_memory/task_YYYYMMDD_HHMM_tahap12_script_driven_ui.md` setelah tahap ini selesai.
2. **Build Verification:**
   - Pastikan setiap perubahan dikonfirmasi dengan `./gradlew assembleDebug` tanpa *error* kompilasi sebelum kamu menyatakan tugas selesai.

---

# Feature Requirements Analysis
Kita perlu membuat jembatan UI (`RoCatUI`) yang disuntikkan ke dalam *Rhino Engine* agar skrip JS bisa memanggil komponen UI Compose secara dinamis.

1. **`RoCatUI` Native Bridge:**
   Skrip JS harus bisa memanggil fungsi berikut:
   - `RoCatUI.addInput(id, hint)`: Menampilkan *text field*.
   - `RoCatUI.addButton(label, functionName)`: Menampilkan tombol yang jika diklik akan mengeksekusi fungsi JS bernama `functionName`.
   - `RoCatUI.thumbnailPreview(url)`: Menampilkan gambar (*image rendering*) menggunakan Coil.
   - `RoCatUI.videoPreview(url)`: Menampilkan *card/button* untuk memutar video (memicu `Intent.ACTION_VIEW`).
   - `RoCatUI.clear()`: Membersihkan seluruh komponen UI dari layar.
   - `RoCatUI.log(text)`: Menambah teks ke area log.

2. **State Management (ViewModel & Compose):**
   - Buat *sealed class* `UIComponent` di Kotlin (contoh: `Input(id, hint)`, `Button(label, onClick)`, `Thumbnail(url)`, `Video(url)`).
   - `PlaygroundViewModel` harus menyimpan `SnapshotStateList<UIComponent>`.
   - Di Compose, lakukan *looping* (misal dengan `LazyColumn`) terhadap state komponen tersebut dan *render* komponen yang sesuai.
   - Saat tombol ditekan, kumpulkan semua nilai dari komponen `Input`, jadikan satu objek JSON/Map, dan teruskan sebagai argumen ke `ExecuteScript.invoke(..., functionName, inputs)`.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 12.1: Buat UI State & `RoCatUI` Bridge
1. Buat model data untuk UI (misal di folder `domain` atau `presentation`):
   ```kotlin
   sealed class ScriptUIComponent {
       data class Input(val id: String, val hint: String, var value: String = "") : ScriptUIComponent()
       data class Button(val label: String, val functionName: String) : ScriptUIComponent()
       data class Thumbnail(val url: String) : ScriptUIComponent()
       data class Video(val url: String) : ScriptUIComponent()
       data class LogText(val text: String) : ScriptUIComponent()
   }

```
 2. Buat *class* UiBridge yang menyimpan *reference* ke fungsi *callback* di ViewModel untuk menambah komponen ini ke state Compose. Ekspos *class* ini ke Rhino dengan nama RoCatUI.
### Tahap 12.2: Rombak PlaygroundScreen.kt & ViewModel
 1. Hapus logika *Function Selector* dan *Input Arg* statis dari Tahap 11.
 2. Di saat skrip pertama kali di-load di Playground, otomatis panggil fungsi buildUI() dari JS (jika ada) untuk melakukan *initial render*.
 3. Render ScriptUIComponent di layar menggunakan Jetpack Compose:
   * Thumbnail: Gunakan AsyncImage (Coil).
   * Video: Gunakan OutlinedButton berikon Play, gunakan Intent.ACTION_VIEW ke URL saat diklik.
   * Input: Gunakan OutlinedTextField.
 4. Saat komponen Button diklik: kumpulkan semua properti value dari state Input ke dalam Map<String, String>, lalu panggil invokeFunction JS sesuai functionName milik tombol tersebut.
### Tahap 12.3: Uji Coba dengan Skrip Dinamis
 * Gunakan skrip pengujian ini dan pastikan UI tergambar sesuai instruksi JS:
```javascript
// ==UserScript==
// @name        Script-Driven UI Tester
// @version     1.0.0
// ==/UserScript==

// Fungsi ini akan dipanggil otomatis oleh Kotlin saat ekstensi dibuka
function buildUI() {
    RoCatUI.clear();
    RoCatUI.thumbnailPreview("[https://via.placeholder.com/300](https://via.placeholder.com/300)");
    RoCatUI.addInput("video_url", "Masukkan URL Video / Halaman");
    RoCatUI.addButton("Extract Video", "onExtractClick");
}

function onExtractClick(inputs) {
    // Membaca nilai dari input berdasarkan ID
    var url = inputs.video_url;
    
    if (!url) {
        RoCatUI.log("URL tidak boleh kosong!");
        return;
    }
    
    RoCatUI.log("Memproses: " + url);
    
    // Simulasi hasil scrape
    var extractedVideoUrl = "[https://www.w3schools.com/html/mov_bbb.mp4](https://www.w3schools.com/html/mov_bbb.mp4)";
    
    // Menampilkan video preview di UI
    RoCatUI.videoPreview(extractedVideoUrl);
    RoCatUI.log("Selesai!");
}

```
### Tahap 12.4: Verifikasi & Pembaruan Memori
 * Pastikan gambar *placeholder* muncul, kolom input berfungsi, dan saat tombol diklik, *preview video* serta log muncul.
 * Jalankan ./gradlew :app:assembleDebug.
 * Perbarui status di ai_memory/00_INDEX.md menjadi **Tahap 12 SELESAI** dan buat catatan terperinci.