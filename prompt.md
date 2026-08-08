# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer. Tahap 4 untuk `rocat-app` telah dilakukan, namun aplikasi **masih mengalami Force Close (FC)** di perangkat nyata/emulator, dan log error tidak terlihat.

Tugasmu di **Tahap 5** adalah membuat **Global Crash Handler**. Jika aplikasi *crash*, aplikasi tidak boleh langsung tertutup rapat, melainkan harus masuk ke halaman khusus (Crash Log Screen) yang menampilkan detail *stack trace*, bisa disalin (copy), dan menyimpan log tersebut ke dalam penyimpanan lokal (`Android/data/`).

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami seluruh protokol manajemen memori, pembatasan token, dan aturan penulisan log secara ketat.
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan tugas setelah tahap ini selesai.
2. **Build Verification:**
   - Pastikan setiap perubahan kode/sub-tahap dikonfirmasi dengan `./gradlew assembleDebug` untuk menjamin tidak ada *error* kompilasi. Jangan lanjut ke tahap berikutnya jika *build* gagal.

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 5.1: Global Exception Handler & File Logging
1. **Buat File Logger:**
   - Buat fungsi/utilitas untuk menangkap *stack trace* (Throwable) dan menyimpannya ke dalam file teks (misalnya `crash_log_[timestamp].txt`).
   - **Wajib:** Simpan file log ini ke direktori `context.getExternalFilesDir(null)` agar tersimpan di `Android/data/[package.name]/files/` yang mudah diakses tanpa memerlukan *runtime permission* khusus.
2. **Set Default Uncaught Exception Handler:**
   - Di dalam kelas `Application` (`RoApp.kt`), ambil alih `Thread.setDefaultUncaughtExceptionHandler`. 
   - Ketika *crash* terjadi, simpan log menggunakan utilitas di atas, lalu luncurkan `CrashActivity` (buat di tahap selanjutnya) menggunakan `Intent` dengan *flag* `FLAG_ACTIVITY_NEW_TASK` dan `FLAG_ACTIVITY_CLEAR_TASK`, sebelum mematikan proses utama (`Process.killProcess`).

### Tahap 5.2: UI Crash Log (CrashActivity)
1. **Buat `CrashActivity` Terpisah:**
   - Buat Activity baru khusus untuk menampilkan *crash*. **Jangan** gabungkan ke dalam `RoCatNav` atau `MainActivity` karena jika `MainActivity` yang menjadi sumber *crash*, layar *crash* tidak akan bisa dimuat.
2. **Desain Halaman (Compose):**
   - Halaman harus menerima data *stack trace* (via `Intent` string atau membaca file log terakhir).
   - Tampilkan teks *stack trace* di dalam area yang bisa di-*scroll*.
   - Sediakan tombol **"Copy to Clipboard"** agar pengguna bisa menyalin log dengan mudah.
   - Sediakan informasi path tempat file log disimpan (misal: "Log saved to Android/data/...").

### Tahap 5.3: Integrasi & Verifikasi
1. **Daftarkan di Manifest:**
   - Pastikan `CrashActivity` didaftarkan di `AndroidManifest.xml`.
2. **Build Check:**
   - Lakukan `./gradlew assembleDebug` dan pastikan semuanya terkompilasi tanpa error.

**Instruksi Eksekusi:** Konfirmasi bahwa kamu membaca `memory_prompt.md`. Mulai kerjakan dari Tahap 5.1. Berikan penjelasan singkat tentang implementasi `UncaughtExceptionHandler` yang kamu buat, dan pastikan tidak ada *error build* sebelum melaporkan penyelesaian.
