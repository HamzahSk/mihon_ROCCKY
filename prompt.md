
# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Kita perlu melakukan **Hotfix & Refinement UI Playground (Tahap 11.5)** karena implementasi Tahap 11 sebelumnya memiliki beberapa bug UX dan logika yang kurang optimal.

# Bug & UI Requirements Analysis
Berdasarkan pengujian manual dan *screenshot*, terdapat masalah berikut di `PlaygroundScreen`:
1. **Bagian "Run main" Redundan:** Ada *card* "Run main" (Target URL) di bagian atas yang sudah tidak diperlukan karena kita sudah punya "Test Function" di bawahnya. Ini harus dihapus.
2. **Auto-Detect Function Names:** *Dropdown/Function Selector* saat ini tidak mendeteksi fungsi yang ada di dalam skrip. Seharusnya ViewModel memindai kode skrip (misal menggunakan Regex `function\s+([a-zA-Z_$][\w$]*)\s*\(`) dan menampilkan daftar fungsi tersebut secara otomatis di *dropdown*.
3. **Input Fleksibel & Cerdas:** Baris input "Key" dan "Value" default tidak boleh muncul jika skrip tidak membutuhkannya. Secara *default*, state *arguments* harus kosong (`emptyList()`). Jika *user* butuh argumen, barulah mereka menekan tombol "+ Add Input". Contoh: skrip yang hanya memiliki `function main()` tanpa parameter harus bisa langsung di-run tanpa terganggu oleh *form* input kosong.
4. **Log/Result JSON Pretty Print & Selectable:** - Hasil *output* JSON saat ini hanya berupa *raw string* (minified). Perbaiki `ResultFormatter.kt` agar benar-benar melakukan *pretty-print* (Parse *raw string* ke `JsonElement` lalu *encode* ulang dengan `Json { prettyPrint = true }`).
   - Teks *output* di dalam *Log Card* wajib dibungkus dengan `SelectionContainer { ... }` (Jetpack Compose) agar pengguna bisa menyeleksi (*highlight*) dan menyalin sebagian teks secara manual.

# Execution Plan (Tahap 11.5)

### 1. Update `PlaygroundViewModel.kt`
- Hapus *state* atau fungsi yang berkaitan dengan *card* "Run main" lama jika masih ada.
- Tambahkan fungsi `extractFunctionNames(scriptCode: String): List<String>` menggunakan Regex untuk mencari nama-nama fungsi di dalam skrip.
- Saat skrip dimuat, panggil fungsi tersebut dan jadikan hasilnya sebagai opsi di *Function Selector* UI. Set fungsi pertama (jika ada) sebagai *default selected function*.
- Ubah inisialisasi `testArgs` menjadi *list* kosong secara *default*.

### 2. Update `PlaygroundScreen.kt`
- **HAPUS** *Card* "Run main" yang berada di atas. Sisakan bagian *Test Function* dan *Log/Result* saja.
- Tampilkan daftar input *Key/Value* **hanya** jika `testArgs` tidak kosong. 
- Hubungkan *Dropdown Menu* dengan *list* fungsi hasil deteksi dari ViewModel.
- Bungkus komponen `Text` yang menampilkan hasil eksekusi (di dalam `CopyableResultCard` atau *Log area*) dengan `SelectionContainer` dari Jetpack Compose.

### 3. Update `ResultFormatter.kt`
- Perbaiki logika `prettyJson()`. Jika hasil dari Rhino adalah *raw JSON string*, parse terlebih dahulu:
  ```kotlin
  try {
      val jsonElement = Json.parseToJsonElement(rawString)
      val format = Json { prettyPrint = true }
      return format.encodeToString(JsonElement.serializer(), jsonElement)
  } catch (e: Exception) {
      return rawString // Fallback jika bukan JSON valid
  }

```
### 4. Build & Verifikasi
 * Uji eksekusi menggunakan contoh skrip ini (di mana main tidak memiliki argumen, jadi bisa langsung di-run):
```javascript
// ==UserScript==
// @name        MythToons HTML Tester
// @version     1.0.0
// @description Fetch & test HTML from MythToons (sync fetch + RoCatDOM).
// @author      Tester
// @match       [https://mythtoons.org/](https://mythtoons.org/)*
// @grant       none
// ==/UserScript==

function main() {
    return testHtml();
}

function testHtml() {
    var url = "[https://mythtoons.org/](https://mythtoons.org/)";
    var res = fetch(url, "GET", {}, null);
    if (!res.ok) { return { error: "HTTP " + res.status, message: res.body }; }
    var html = res.text();
    return parseTest(html, url);
}

function parseTest(html, url) {
    var root = RoCatDOM.parse(html);
    var titleEl = root.find("title");
    return {
        test_url: url,
        page_title: titleEl.length > 0 ? titleEl[0].text : "Title tidak ditemukan",
        html_length: html.length,
        html_preview: html.substring(0, 500) 
    };
}

```
 * Pastikan ./gradlew assembleDebug berhasil.
 * Perbarui log memori di ai_memory/00_INDEX.md dengan status **Tahap 11.5 SELESAI** dan catat perubahan ini.
Tolong kerjakan sekarang dan berikan laporan jika sudah selesai!