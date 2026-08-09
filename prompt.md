# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Kita sekarang masuk ke **Tahap 16: Perbaikan Storage & Clear Data, serta Implementasi In-App Browser Bebas**.
Fokus tahap ini adalah memperbaiki *bug* kritis di mana hasil *scrape* gagal tersimpan ke dalam memori perangkat, memastikan fitur "Clear Cache & Cookies" benar-benar menghapus data hingga ke akar (termasuk WebView), dan merombak tab navigasi dengan menghapus *Playground* lalu menggantinya dengan fitur peramban web (*Browser*) penuh yang bisa membuka URL apapun secara bebas.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan di `ai_memory/task_YYYYMMDD_HHMM_tahap16_bugfixes_and_browser.md` setelah tahap ini selesai.
2. **Context Path (SANGAT PENTING):**
   - Proyek ini berada di dalam *sub-directory* `rocat-app/`.
   - **SEMUA** modifikasi file **WAJIB** dilakukan di dalam folder `rocat-app/`.
3. **Jetpack Compose & Modern Android:**
   - Gunakan Jetpack Compose untuk UI `BrowserScreen`.
   - Modifikasi interaksi penyimpanan menggunakan `DocumentFile` dan `ContentResolver.openOutputStream`.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 16.1: Perbaikan Bug Storage (Save Scraped Files)
- Masalah saat ini: `StorageManager.createScrapeFolder()` berhasil membuat sub-folder, tetapi file hasil *scrape* tidak tersimpan di dalamnya.
- **Solusi:** Buat fungsi eksekusi atau utilitas di `StorageManager` (misal: `saveFileToScrapeFolder(folderUri, fileName, mimeType, content/bytes)`). 
- Fungsi ini harus menggunakan `DocumentFile.fromTreeUri`, memanggil `createFile()`, lalu menggunakan `context.contentResolver.openOutputStream(uri)` untuk menulis data ke dalam file tersebut.
- Pastikan modul *scripting* menggunakan fungsi baru ini agar file benar-benar tertulis ke *storage*.

### Tahap 16.2: Perbaikan Bug Clear Data (Cache & Cookies)
- Masalah saat ini: Menghapus data dari DAO Room (`CookieDao`) dan direktori standar tidak cukup membersihkan sesi web secara nyata.
- **Solusi Cookies:** Di fungsi penghapusan *cookie* (di `SettingsViewModel`), selain `cookieDao.deleteAll()`, panggil juga `android.webkit.CookieManager.getInstance().removeAllCookies(null)` dan `flush()` agar *cookie* WebView bawaan terhapus tuntas.
- **Solusi Cache:** Tambahkan logika untuk menghapus *cache* WebView melalui `WebView(context).clearCache(true)`.

### Tahap 16.3: Refaktor Navigasi (Hapus Playground)
- Buka file navigasi utama (misal: `RoCatNav.kt`).
- Hapus total semua referensi ke `Screen.Playground`, rute, *string resource* (i18n), dan ikonnya.
- Ganti dengan tab baru: `Screen.Browser`. Pastikan ikon navigasi di *bottom bar* menggunakan ikon web (misal: `Icons.Filled.Public` atau `Language`).

### Tahap 16.4: Implementasi BrowserScreen (Freestyle Web Browser)
- Buat file `BrowserScreen.kt` menggunakan Jetpack Compose.
- **UI Address Bar Bebas:** Buat *top bar* yang berisi kolom input URL (`OutlinedTextField` atau `BasicTextField`) dan tombol *Enter/Go*. Pengguna harus bisa mengetikkan URL APAPUN (misal `https://google.com`) dan melakukan *browsing* sebebas-bebasnya layaknya aplikasi Chrome. Ini murni fitur *browser* mandiri dan tidak menjalankan *scrape*.
- **Kontrol Navigasi:** Sediakan tombol *Back* (untuk mundur halaman web), *Forward*, *Refresh*, dan *Stop*.
- **Engine WebView:** Gunakan komponen `AndroidView` untuk me-*render* WebView. Pastikan WebView ini memuat URL yang diketik pengguna.
- **Sinkronisasi Cookie (Nilai Plus):** Karena kita menggunakan `AndroidCookieJar`, *cookie* dari sesi *login* pengguna di *browser* bebas ini (misal login akun atau lewat Cloudflare) akan otomatis tersimpan ke `CookieManager` dan bisa dinikmati oleh mesin *scraper* kita di belakang layar.

### Tahap 16.5: Verifikasi Build & Update Memory
- Jalankan `cd rocat-app && ./gradlew :app:assembleDebug` untuk memastikan tidak ada *build error*.
- Perbarui file `00_INDEX.md` dengan status **Tahap 16 SELESAI** dan rangkum perubahannya.
