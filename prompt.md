# Role and Objective
Kamu adalah AI Software Engineer dan Technical Writer handal. Kita sekarang masuk ke **Tahap 23: Pembaruan Dokumentasi Scripting & Perbaikan Skrip Scraper (`testscrape.txt`)**.
Fokus tahap ini adalah memperbarui file `DOCS_SCRIPTING.md` agar mencakup semua penyederhanaan API dan komponen UI baru dari Tahap 22, serta menganalisis dan memperbaiki kode *scraper* dari file `testscrape.txt` hingga berhasil dieksekusi dengan mulus menggunakan format API terbaru.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan di `ai_memory/task_YYYYMMDD_HHMM_tahap23_docs_update_and_script_fix.md` setelah tahap ini selesai.
2. **Context Path:**
   - File dokumentasi `DOCS_SCRIPTING.md` berada di *root* proyek `rocat-app/`.
   - Pastikan kamu membaca file `testscrape.txt` yang dilampirkan sebelum memperbaikinya.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 23.1: Analisis Skrip `testscrape.txt`
- Baca isi file `testscrape.txt`.
- Identifikasi masalah utama pada skrip tersebut (apakah masalah *parsing* DOM, struktur JSON, pemanggilan API lama, atau *error logic*).
- Pahami target situs/API yang sedang di-*scrape* oleh skrip tersebut untuk memastikan logika penggantinya nanti akurat.

### Tahap 23.2: Perbaikan Skrip & Migrasi ke API Baru
- Tulis ulang dan perbaiki skrip tersebut. Ubah ekstensinya atau simpan sebagai file `.js` yang valid (misalnya `fixed_testscrape.js`).
- **Terapkan API Tahap 22:**
  - Gunakan helper global `RoCat` (seperti `RoCat.safeParseJson` atau `RoCat.fetchJson` jika relevan).
  - Ganti pemanggilan UI manual yang rumit dengan komponen baru (misalnya gunakan `RoCatUI.addAlert()` untuk *error handling*, `RoCatUI.addBadgeGroup()` untuk genre/status, atau `RoCatUI.addJsonLog()` untuk *debugging* hasil API).
- Pastikan *lifecycle* skrip (seperti `onLaunch()`) dan parsing HTML (`RoCatDOM`) sudah digunakan dengan format yang paling sederhana dan kebal *error* (*fault-tolerant*).

### Tahap 23.3: Pembaruan File `DOCS_SCRIPTING.md`
- Buka dan edit file `DOCS_SCRIPTING.md`.
- **Tambahkan Bagian Wrapper Helper (`RoCat`):** Dokumentasikan fungsi `RoCat.render()`, `RoCat.safeParseJson()`, dan `RoCat.fetchJson()`.
- **Tambahkan Komponen UI Baru:** Jelaskan penggunaan dan parameter untuk:
  - `RoCatUI.addJsonLog(dataJson, title, allowCopy)`
  - `RoCatUI.addHtmlPreview(htmlContent, title)`
  - `RoCatUI.addAudio(url, title, allowDownload)`
  - `RoCatUI.addAlert(message, type)`
  - `RoCatUI.addBadgeGroup(badgesJson)`
- Perbarui contoh *boilerplate* skrip di dalam dokumentasi agar mencerminkan gaya penulisan skrip gaya baru yang lebih bersih.

### Tahap 23.4: Pengujian & Validasi
- Simulasikan eksekusi skrip hasil perbaikan (`fixed_testscrape.js`) di dalam otak / test *environment* kamu.
- Jika memungkinkan, jalankan unit test di `scripting/rhino` menggunakan skrip baru ini untuk memastikan tidak ada *syntax error* dan semua *bridge native* terpanggil dengan benar.

### Tahap 23.5: Update Memory
- Perbarui file `00_INDEX.md` dengan status **Tahap 23 SELESAI**.
- Buat catatan teknis di `task_YYYYMMDD_HHMM_tahap23_docs_update_and_script_fix.md` yang merangkum apa saja yang salah dari skrip awal dan bagaimana kamu memperbaikinya.
