# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Kita sekarang masuk ke **Tahap 15: Lokalisasi (i18n), Scoped Storage, Database, & Pengaturan**.
Fokus tahap ini adalah menambahkan dukungan multi-bahasa menggunakan arsitektur `i18n` kustom, meminta akses direktori utama menggunakan Storage Access Framework (seperti Mihon), mengatur struktur folder untuk hasil *scrape*, menginisiasi Room Database (SQLite), dan membuat halaman Pengaturan (Settings) komprehensif.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `rocat-app/ai_memory/00_INDEX.md` dan membuat catatan di `rocat-app/ai_memory/task_YYYYMMDD_HHMM_tahap15_localization_storage_db.md` setelah tahap ini selesai.
2. **Context Path (SANGAT PENTING):**
   - Proyek ini berada di dalam *sub-directory* `rocat-app/`.
   - **SEMUA** modifikasi file **WAJIB** dilakukan di dalam folder `rocat-app/`.
3. **Jetpack Compose & Modern Android:**
   - Gunakan Jetpack Compose untuk UI Pengaturan.
   - Gunakan Room untuk Database.
   - Gunakan Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`) dan `DocumentFile` untuk mengelola folder.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 15.1: Lokalisasi Custom (Folder i18n)
- Jangan gunakan standar `res/values/strings.xml`. Sebagai gantinya, buat implementasi lokalisasi di dalam folder/package `i18n` (misalnya membuat struktur package Kotlin `app/rocat/i18n/` berisi objek *string* atau menyimpan file terjemahan di `rocat-app/app/src/main/assets/i18n/`).
- Buat *base language* (English `en`) dan bahasa Indonesia (`id`).
- Buat *helper* atau *provider* di Jetpack Compose agar UI bisa reaktif saat bahasa diganti.
- Pindahkan *hardcoded strings* utama yang sudah ada di UI (seperti judul aplikasi, tombol navigasi) ke dalam sistem `i18n` ini.

### Tahap 15.2: Storage Access Framework & Manajemen Folder Scrape
- Buat logika untuk mendeteksi apakah aplikasi sudah memiliki *URI permission* untuk direktori utamanya (simpan statusnya di `DataStore` atau `SharedPreferences`).
- Jika pada saat *first launch* direktori belum di-set, tampilkan UI (Dialog atau Screen khusus) yang menyuruh pengguna memilih folder utama aplikasi (mirip mekanisme Mihon).
- Gunakan `rememberLauncherForActivityResult` dengan `ActivityResultContracts.OpenDocumentTree()` di Compose untuk memunculkan pemilih folder.
- Simpan *URI path* dan ambil *persistable URI permission*.
- **Struktur Folder Scrape:** Buat fungsi utilitas menggunakan `DocumentFile` untuk membuat sub-folder baru di dalam direktori utama setiap kali proses *scrape* berjalan (misal: `[Direktori_Utama]/Scrapes/[Nama_atau_ID_Scrape]/`). Semua file hasil *scrape* terkait harus disimpan di dalam sub-folder spesifik ini.

### Tahap 15.3: Inisialisasi Room Database (SQLite)
- Buka `rocat-app/gradle/libs.versions.toml` dan tambahkan dependensi untuk **Room** (`androidx.room`).
- Implementasikan di `build.gradle.kts` (gunakan KSP untuk *annotation processing* Room).
- Buat entitas dasar:
  - `CookieEntity`: Untuk menyimpan data cookie aplikasi/script.
  - `HistoryEntity`: Untuk menyimpan riwayat penggunaan/bacaan.
- Buat DAO (Data Access Object) dan `AppDatabase`.
- Integrasikan dengan Dependency Injection (atau buat *singleton instance*).

### Tahap 15.4: Tab Pengaturan (Settings Screen)
- Tambahkan rute navigasi baru untuk `SettingsScreen`.
- Buat UI Settings menggunakan Jetpack Compose.
- Tambahkan fitur-fitur berikut di halaman pengaturan:
  - **Bahasa:** Opsi untuk mengganti bahasa aplikasi (men-*trigger* sistem `i18n` yang dibuat di Tahap 15.1).
  - **Ubah Direktori Penyimpanan:** Tombol untuk memanggil ulang *launcher* `OpenDocumentTree()` guna mengubah folder utama.
  - **Hapus Cache:** Tombol untuk menghapus *cache* aplikasi. Ini harus membersihkan *cache* memori dan disk dari **Coil** (image loader) serta menghapus isi dari direktori `context.cacheDir`.
  - **Hapus Cookie:** Tombol untuk mengeksekusi *query delete all* pada `CookieEntity` di Room Database.
  - **Hapus Riwayat:** Tombol untuk mengeksekusi *query delete all* pada `HistoryEntity`.

### Tahap 15.5: Verifikasi & Pembaruan Memori
- Jalankan `cd rocat-app && ./gradlew :app:assembleDebug` untuk memastikan implementasi Room, KSP, dan sistem lokalisasi yang baru tidak menyebabkan *build error*.
- Perbarui `00_INDEX.md` menjadi **Tahap 15 SELESAI** beserta ringkasan teknisnya.
