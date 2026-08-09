# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Kita sekarang masuk ke **Tahap 22: Simplifikasi API Scripting, Standardisasi Format Universal, & Ekspansi Template UI (JSON Viewer, HTML Preview, Audio, Alert, Badge)**.
Fokus tahap ini adalah menyederhanakan format penulisan skrip agar lebih umum, toleran terhadap kesalahan data (*fault-tolerant*), serta memperkaya variasi template UI bawaan yang dapat dipanggil langsung dari skrip JS.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan di `ai_memory/task_YYYYMMDD_HHMM_tahap22_script_simplification_and_ui_templates.md` setelah tahap ini selesai.
2. **Context Path:**
   - Semua modifikasi kode Kotlin dan skrip JS berada di dalam sub-direktori `rocat-app/`.
3. **Jetpack Compose & Rhino Compatibility:**
   - Buat komponen Compose baru yang responsif, rapi, dan sesuai dengan sistem tema RoCat.
   - Pastikan wrapper JS baru tetap kompatibel dengan Rhino Engine (mode interpretasi, tanpa ES6+ yang tidak didukung seperti `async/await` atau `class`).

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 22.1: Injeksi JS Core Wrapper (Universal API Abstraction)
- **Auto-Injected Helper (`RoCat`):** Buat / suntikkan helper JS global (misalnya `RoCat`) secara otomatis sebelum skrip pengguna dieksekusi di Rhino Engine.
- **Fitur Wrapper Helper:**
  - `RoCat.render(items)`: Menerima *array* atau objek UI secara fleksibel tanpa perlu memanggil banyak fungsi `RoCatUI.add...` satu per satu.
  - `RoCat.safeParseJson(str, fallback)`: Utilitas internal agar parsing JSON tidak pernah membuat skrip *crash*.
  - `RoCat.fetchJson(url, options)`: Wrapper otomatis untuk `fetch()` yang langsung mengembalikan objek JSON ter-parse secara aman.
  - **Toleransi Input:** Pastikan bridge `RoCatUI` di Kotlin secara otomatis menoleransi argumen yang `null`, `undefined`, atau salah tipe data (memberikan nilai *default* aman alih-alih melempar *exception*).

### Tahap 22.2: Ekspansi Komponen Template UI Baru (`ScriptUIComponent`)
Tambahkan komponen UI baru pada sealed class `ScriptUIComponent` di Kotlin dan bridge `ScriptUiBridge`:

1. **JSON Log Viewer (`RoCatUI.addJsonLog(dataJson, title, allowCopy)`):**
   - Kartu tampilan data JSON yang rapi (*pretty printed*), berbunga warna (*syntax highlighting* sederhana / monospaced), dilengkapi tombol **"Copy JSON"** dengan indikator Toast.
2. **HTML Preview (`RoCatUI.addHtmlPreview(htmlContent, title)`):**
   - Komponen untuk menampilkan deskripsi/pengumuman kaya teks (Rich Text / WebView ringkas / AnnotatedString HTML) untuk *formatting* seperti bold, italic, link, dan list.
3. **Audio Player (`RoCatUI.addAudio(url, title, allowDownload)`):**
   - Pemutar media audio ringkas (menggunakan Media3 ExoPlayer) lengkap dengan kontrol Play/Pause, Progress Bar, dan tombol unduh SAF.
4. **Alert / Banner Card (`RoCatUI.addAlert(message, type)`):**
   - Kartu notifikasi/peringatan dengan tipe: `"info"`, `"warning"`, `"error"`, `"success"`. Dilengkapi ikon dan warna latar belakang yang disesuaikan.
5. **Chip / Badge Group (`RoCatUI.addBadgeGroup(badgesJson)`):**
   - Menampilkan deretan status/kategori (misalnya: `["Ongoing", "HD", "Action", "Rating 8.5"]`) dalam bentuk *FlowRow* chip yang rapi.

### Tahap 22.3: Implementasi Native Compose UI & Visual Polish
- Buat file composable baru di `app/rocat/ui/components/` untuk masing-masing template di atas:
  - `JsonLogCard.kt`
  - `HtmlPreviewCard.kt`
  - `AudioPreviewCard.kt`
  - `AlertBannerCard.kt`
  - `BadgeGroupCard.kt`
- Daftarkan komponen-komponen baru ini ke dalam LazyColumn di `ScriptCanvasScreen.kt`.

### Tahap 22.4: Penyederhanan Skrip Demo & `scrape_anichin.js`
- Terapkan format API penyederhanaan baru pada skrip `scrape_anichin.js` agar kodenya lebih ringkas, mudah dibaca, dan tidak rawan eror.
- Manfaatkan komponen UI baru seperti `addAlert` untuk pesan *status/error* dan `addBadgeGroup` untuk menampilkan genre/status episode.

### Tahap 22.5: Verifikasi Build, Unit Test, & Update Memory
- Buat unit test baru di `scripting/rhino` untuk menguji pemanggilan komponen UI baru dari skrip JS.
- Jalankan `./gradlew :app:assembleDebug` dan unit test untuk memastikan build dan eksekusi berjalan tanpa masalah.
- Perbarui file `00_INDEX.md` dengan status **Tahap 22 SELESAI** dan buat catatan teknis di `task_YYYYMMDD_HHMM_tahap22_script_simplification_and_ui_templates.md`.
