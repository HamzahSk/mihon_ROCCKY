# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Kita sekarang masuk ke **Tahap 19: Perbaikan Bug Scraper & Integrasi HLS Stream (Anichin)**.
Fokus tahap ini adalah memperbaiki *bug* pada skrip *scraper* `scrape_anichin.js`, menyesuaikan formatnya dengan struktur asli di `anichin.js`, serta mengintegrasikan pemutar video *native* yang sudah kita buat di Tahap 18 (`RoCatUI.addVideo`) untuk menangani pemutaran *stream* HLS (`.m3u8`) dengan lancar.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan di `ai_memory/task_YYYYMMDD_HHMM_tahap19_bugfix_anichin_stream.md` setelah tahap ini selesai.
2. **Context Path (SANGAT PENTING):**
   - Proyek ini berada di dalam *sub-directory* `rocat-app/`.
   - Modifikasi skrip dilakukan pada folder yang relevan tempat `scrape_anichin.js` disimpan.
3. **Jetpack Compose & Modern Android:**
   - Gunakan API `RoCatUI.addVideo(url, title, isStreamHls, allowDownload)` dari Tahap 18.
   - Pastikan *error handling* pada skrip JS aman agar tidak membuat aplikasi *crash* jika elemen web berubah.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 19.1: Analisis & Sinkronisasi Format Skrip
- Baca dan bandingkan logika *scraping* antara file `anichin.js` (referensi asli) dengan `scrape_anichin.js` (versi RoCat).
- Selaraskan struktur kode `scrape_anichin.js` agar mengikuti standar *lifecycle* RoCat (misalnya menggunakan fungsi `onLaunch()`, `buildUI()`, dll).
- Pastikan penggunaan jembatan UI (seperti `RoCatUI.addInput`, `RoCatUI.addButton`, `RoCatUI.addGrid`) sudah tepat dan efisien.

### Tahap 19.2: Perbaikan Logika Search & Detail
- **Pencarian (Search):** Perbaiki logika ekstraksi data dari hasil pencarian web Anichin. Pastikan *thumbnail*, judul, dan URL detail berhasil diambil dan ditampilkan dengan `RoCatUI.addGrid()`.
- **Detail Anime:** Saat item dari *grid* diklik, pastikan skrip dapat mengambil daftar episode dengan benar. Gunakan fungsi `RoCatUI.addButton()` atau komponen *list* untuk menampilkan pilihan episode.

### Tahap 19.3: Perbaikan Ekstraksi Stream & Integrasi HLS (`RoCatUI.addVideo`)
- **Ekstraksi Video:** Perbaiki fungsi *scraper* untuk menembus *iframe* atau *player* Anichin hingga mendapatkan URL langsung dari video (terutama format `.m3u8` untuk HLS atau `.mp4`).
- **Penerapan UI:** - Jika URL yang didapat adalah *stream* HLS, panggil:  
    `RoCatUI.addVideo(videoUrl, "Judul Episode", true, true);`
  - Jika formatnya MP4 biasa, panggil:  
    `RoCatUI.addVideo(videoUrl, "Judul Episode", false, true);`
- **Headers/Referrer (PENTING):** Analisis apakah URL *stream* membutuhkan *headers* khusus (seperti `Referer` atau `User-Agent`) untuk diputar. Jika ya, terapkan mekanisme pengiriman *header* melalui API `NetworkHelper` atau konfigurasikan `MediaSource` ExoPlayer di sisi native agar menyertakan *header* tersebut saat inisialisasi video.

### Tahap 19.4: Pengujian & Validasi
- Simulasikan atau pastikan skrip yang diperbarui tidak memiliki *syntax error*.
- Verifikasi alur dari: Pencarian -> Klik Detail -> Pilih Episode -> *Card Video* muncul -> Video bisa di-*play* *inline* atau *fullscreen* tanpa *error*.

### Tahap 19.5: Update Memory
- Perbarui file `00_INDEX.md` dengan status **Tahap 19 SELESAI**.
- Buat catatan teknis di `task_YYYYMMDD_HHMM_tahap19_bugfix_anichin_stream.md` berisi kendala yang ditemukan pada DOM Anichin dan cara menyelesaikannya.
