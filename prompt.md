# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer. Tahap 2 dan 3 untuk `rocat-app` telah diimplementasikan (Infrastruktur Script & UI Management ala Mihon). Namun, saat ini aplikasi mengalami **Force Close / Crash** saat pertama kali dibuka.

Tugasmu di **Tahap 4** adalah mencari akar masalah dan memperbaiki *crash* tersebut (Stabilisasi), lalu menyempurnakan alur eksekusi script.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami seluruh protokol manajemen memori, pembatasan token, dan aturan penulisan log secara ketat.
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan tugas setelah tahap ini selesai.
2. **Build Verification:**
   - Pastikan setiap perubahan kode/sub-tahap dikonfirmasi dengan `./gradlew assembleDebug` untuk menjamin tidak ada *error* kompilasi. Jangan lanjut ke tahap berikutnya jika *build* gagal.

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 4.1: Bug Fix & Stabilisasi UI (Prioritas Utama)
1. **Perbaiki Dependency Injection (Injekt):**
   - Periksa `AppModule.kt`. Registrasikan semua ViewModel baru (`ScriptsViewModel`, `ScriptDetailViewModel`, `ImportScriptViewModel`, `PlaygroundViewModel`) menggunakan Injekt factory agar UI tidak *crash* saat memanggilnya.
2. **Review `MainActivity.kt` dan Navigasi:**
   - Pastikan `setContent` di `MainActivity` memanggil `RoCatNav` dengan benar, dan state navigasi tidak memanggil route yang kosong/null.
3. **Amankan File I/O (Repository):**
   - Pastikan `ScriptRepositoryImpl` tidak memblokir baca/tulis file (I/O) di *Main Thread* (misalnya pada fungsi `save` atau inisialisasi). Pindahkan operasi I/O ke `Dispatchers.IO`.

### Tahap 4.2: Penyempurnaan Eksekusi & Error Handling
1. **Playground Runner Enhancement:**
   - Pastikan argumen input URL di Playground benar-benar diteruskan ke dalam fungsi `engine.execute(...)`.
   - Tangkap *error output* (seperti *syntax error* dari eksekusi JS) dan pastikan dirender dengan rapi di UI Playground, jangan sampai membuat *engine crash*.
2. **Script Import Validation:**
   - Saat melakukan import via URL di halaman Import, tambahkan validasi dasar untuk memastikan respons yang diterima adalah *plain text/JS* sebelum menyimpannya ke *storage*.

**Instruksi Eksekusi:** Silakan konfirmasi bahwa kamu telah membaca `memory_prompt.md`. Mulai eksekusi dengan Tahap 4.1. Lakukan perbaikan kode, jelaskan penyebab utama *force close* yang kamu temukan, lalu verifikasi ulang menggunakan `./gradlew assembleDebug` sebelum melaporkan penyelesaian dan menulis log memori.
