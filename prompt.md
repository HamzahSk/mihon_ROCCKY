# Role and Objective
Kamu adalah **Senior Android Engineer, UI/UX Expert, dan Architect** handal. Setelah kita berhasil mengimplementasikan *Dual-Mode Scraping Engine* di Tahap 23, sekarang kita masuk ke **Tahap 24: Modernisasi UI/UX Interaktif & Injeksi HTTP Header (Referer) pada Komponen Media**.

Fokus utama tahap ini adalah:
1. **Modernisasi UI/UX:** Memperbarui antarmuka daftar skrip dan Script Canvas menjadi lebih interaktif, modern (Material Design 3), dengan animasi transisi yang *smooth* dan manajemen *state* yang matang.
2. **Eksekusi Media yang Lebih Matang:** Memodifikasi jembatan UI (`RoCatUI` dan `RoCat.render`) serta komponen *native* untuk menyuntikkan HTTP *header* (khususnya `Referer` berbasis *baseurl*) secara otomatis atau manual saat memuat *thumbnail* gambar (Coil) dan *streaming* video (Media3/ExoPlayer).
3. **Quality Assurance:** Menjamin kualitas kode dengan *formatter* (Spotless) dan memastikan proyek sukses di-*build* tanpa masalah.

---

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan teknis di `ai_memory/task_YYYYMMDD_HHMM_tahap24_modern_ui_and_media_headers.md`.
2. **Context Path:**
   - Pemutar HLS dan Media3 berada di komponen `RocatVideoPlayer.kt`.
   - Jembatan UI *scripting* berada di `ScriptUiBridge.kt` dan API penyederhanaan di `RoCatCoreWrapper.kt`.
   - Rendering UI daftar skrip berada di *layer* Compose.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 24.1: Pembaruan API Jembatan Skrip (Injeksi Header Media)
Banyak *host* gambar dan penyedia HLS/M3U8 memblokir permintaan tanpa *header* `Referer`.
- Modifikasi fungsi `RoCatUI.addImage` dan `RoCatUI.addVideo` (yang sebelumnya hanya menerima parameter seperti `url`, `title`, `allowDownload`, `isStreamHls`) untuk menerima parameter opsional baru berupa objek `headers`.
- **Auto-Fallback BaseURL:** Jika objek `headers` tidak diberikan oleh skrip, mesin harus secara cerdas mengekstrak asal URL (*origin*) atau menggunakan *baseurl* dari metadata skrip sebagai nilai *default* untuk `Referer`.
- Perbarui sistem `RoCat.render` agar *descriptor* `"image"` dan `"video"` dapat menerima atribut `headers`.
  - Contoh: `{ type: "video", url: "https://...", hls: true, headers: {"Referer": "https://anichin.cafe/"} }`.

### Tahap 24.2: Implementasi Native Compose (Coil & ExoPlayer)
Eksekusi data yang dikirim dari Jembatan Skrip ke komponen UI Android *native*.
- **Coil (Thumbnail & Gambar):** Saat membuat `ImageRequest` untuk dirender di *grid* atau kartu gambar, baca objek *headers* dari parameter dan gunakan metode `addHeader("Referer", value)`.
- **Media3/ExoPlayer (Video Stream):** Modifikasi instansiasi `HlsMediaSource` dan `ProgressiveMediaSource`. Gunakan `DefaultHttpDataSource.Factory().setDefaultRequestProperties(headersMap)` untuk memastikan Exoplayer mengirimkan `Referer` saat melakukan *fetch* segmen `.m3u8` dan `.ts`.

### Tahap 24.3: Modernisasi UI/UX (Daftar Skrip & Canvas)
Tingkatkan kematangan desain dari versi sebelumnya:
- **Kartu Skrip (Script List):** Tambahkan efek *ripple* yang lebih halus, indikator status *toggle* (Aktif/Nonaktif) dengan transisi warna yang lebih organik (Material 3 Switch), dan dukungan *swipe-to-delete* atau *swipe-to-edit*.
- **Script Canvas:** 
  - Terapkan animasi `AnimatedVisibility` atau `Crossfade` saat `RoCatUI.clear()` dipanggil dan antarmuka digambar ulang, untuk menghindari *flicker* layar yang kasar.
  - Percantik *template* kartu (seperti `addJsonLog`, `addHtmlPreview`, `addAlert`) dengan sudut melengkung (*rounded corners*), *elevation/shadow* dinamis, dan tipografi yang lebih modern.

### Tahap 24.4: Pembaruan Skrip Contoh & Dokumentasi `DOCS_SCRIPTING.md`
- Perbarui dokumentasi resmi di bab **Dokumentasi UI Bridge — global `RoCatUI`**.
- Tambahkan penjelasan mengenai penambahan argumen `headers` pada `addImage` dan `addVideo`.
- Sertakan contoh skrip (*boilerplate*) yang menunjukkan cara meneruskan parameter `Referer` khusus ketika memuat *iframe* atau memutar URL dari `html5player`.

### Tahap 24.5: Testing & Update Memory
- Verifikasi bahwa gambar yang dilindungi *hotlink protection* sekarang dapat dimuat di *grid* melalui Coil berkat *header* `Referer`.
- Uji pemutar video dengan sumber `.m3u8` ketat untuk memastikan ExoPlayer tidak mendapatkan *error* HTTP 403.
- Perbarui `ai_memory/00_INDEX.md` dengan menandai Tahap 24 selesai.

### Tahap 24.6: Code Formatting (Spotless) & Build Verification
- Jalankan task *formatter* (contoh: `./gradlew spotlessApply` atau `ktlintFormat`) untuk memastikan standar penulisan kode tetap bersih dan seragam.
- Lakukan proses kompilasi menyeluruh (contoh: `./gradlew assembleDebug` atau `./gradlew build`) untuk memastikan tidak ada konflik dependensi atau *error compile* pada modifikasi Compose dan Media3 yang baru ditambahkan.
