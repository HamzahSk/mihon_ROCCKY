# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Kita sekarang masuk ke **Tahap 18: Media Template, Downloader (Image & Video), serta HLS Video Streaming (ExoPlayer/Media3)**.
Fokus tahap ini adalah menambahkan komponen UI/Template media yang kaya untuk gambar dan video (dilengkapi tombol simpan/download ke *storage* SAF), serta mengintegrasikan pemutar video *native* berkinerja tinggi menggunakan **AndroidX Media3 (ExoPlayer)** yang mendukung *streaming* protokol HLS (`.m3u8`) secara *full screen*.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan di `ai_memory/task_YYYYMMDD_HHMM_tahap18_media_downloader_hls_stream.md` setelah tahap ini selesai.
2. **Context Path (SANGAT PENTING):**
   - Proyek ini berada di dalam *sub-directory* `rocat-app/`.
   - **SEMUA** modifikasi file **WAJIB** dilakukan di dalam folder `rocat-app/`.
3. **Jetpack Compose & Modern Android:**
   - Gunakan `androidx.media3` (ExoPlayer & UI) untuk pemutaran video HLS/MP4.
   - Integrasikan tombol Download dengan `StorageManager.saveFileToScrapeFolder()` yang sudah diperbaiki di Tahap 16/17.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 18.1: Template Image Preview & Downloader
- **Komponen UI:** Perbarui/buat komponen `ImagePreviewCard` di Canvas/Bridge `RoCatUI`.
- **Fitur Download:** Tambahkan tombol ikon "Download" / "Save to Storage" pada setiap pratinjau gambar.
- **Logika Unduh:** Saat tombol diklik, sistem mengunduh gambar via OkHttp (`NetworkHelper`), lalu menyimpannya ke folder *scrape* aktif via `StorageManager.saveFileToScrapeFolder(folder, fileName, mimeType, bytes)`.
- Tampilkan indikator *loading* singkat / *progress bar* dan Toast konfirmasi ("Gambar berhasil disimpan di folder Scrapes/[Script_ID]").

### Tahap 18.2: Template Video Preview & Downloader (TikTok / Video Downloader)
- **Komponen UI:** Buat/perbarui komponen `VideoPreviewCard` yang menampilkan kartu pratinjau video (thumbnail + judul/informasi) lengkap dengan tombol **Play Inline** dan **Download Video**.
- **Fitur Download Video:** Tombol "Download Video" mengunduh file MP4/video secara *asynchronous* di *background* (`Dispatchers.IO`), menampilkan *progress* unduhan, dan menyimpan file video langsung ke folder *scrape* SAF skrip.
- Ini memungkinkan skrip jenis *TikTok / Reel / Video Downloader* dapat langsung memutar video hasil *scrape* sekaligus mengunduhnya dengan sekali klik.

### Tahap 18.3: Integrasi AndroidX Media3 (ExoPlayer) & Support HLS Streaming (`.m3u8`)
- **Dependensi:** Tambahkan AndroidX Media3 ke `gradle/libs.versions.toml` dan `app/build.gradle.kts`:
  - `androidx.media3:media3-exoplayer:1.4.1` (atau versi stabil terbaru)
  - `androidx.media3:media3-exoplayer-hls:1.4.1`
  - `androidx.media3:media3-ui:1.4.1`
- **Player Component (`RocatVideoPlayer`):** Buat komponen Compose berbasis `AndroidView` yang membungkus `PlayerView` Media3.
- Konfigurasikan `ExoPlayer` agar mendukung *source* video standar (MP4/WebM) dan *source* HLS (`HlsMediaSource` untuk URL `.m3u8`).
- **Mode Full Screen:**
  - Sediakan tombol *Full Screen* pada kontrol pemutar video.
  - Saat mode *Full Screen* aktif, orientasi layar beralih secara otomatis ke *landscape*, menyembunyikan *System Bars* (mode *immersive*), dan menampilkan video memenuhi layar.
  - Saat keluar dari *Full Screen* atau menekan tombol *Back*, orientasi kembali ke *portrait* dan *System Bars* muncul kembali.

### Tahap 18.4: Integrasi Native Bridge `RoCatUI`
- Perluas bridge `RoCatUI` / `ScriptUiBridge` agar skrip JS dapat memanggil metode media baru ini:
  - `RoCatUI.addImage(url, title, allowDownload)`
  - `RoCatUI.addVideo(url, title, isStreamHls, allowDownload)`
- Pastikan saat skrip memanggil `addVideo(url, title, true, true)`, aplikasi langsung menampilkan pemutar ExoPlayer yang siap memutar *stream* `.m3u8` serta menyediakan tombol unduh.

### Tahap 18.5: Verifikasi Build & Update Memory
- Jalankan `cd rocat-app && ./gradlew :app:assembleDebug` untuk memastikan dependensi Media3 dan logika *full screen* tidak menyebabkan masalah *build*.
- Perbarui file `00_INDEX.md` dengan status **Tahap 18 SELESAI** dan buat catatan teknis di `task_YYYYMMDD_HHMM_tahap18_media_downloader_hls_stream.md`.
