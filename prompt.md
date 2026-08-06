# Task: Fix Double Image Request Bug (WP Proxy) on Reader and Downloader

## 1. Context & Bug Description
Saat ini, ekstensi `Cgbum.kt` memiliki fitur opsional untuk menggunakan WordPress Proxy (`i0.wp.com`) yang bertujuan mengompresi ukuran gambar agar lebih hemat kuota pengguna. 
* **Masalah:** Saat fitur proxy aktif, aplikasi (Mihon/Tachiyomi) terpantau melakukan *double request* (memuat gambar dua kali) baik saat **membaca komik secara langsung (*reader*)** maupun saat **mengunduh chapter (*downloader*)**.
* Aplikasi me-request URL proxy (CORS) **DAN** URL gambar aslinya secara bersamaan atau berurutan.
* Akibatnya, alih-alih menghemat data, penggunaan kuota malah menjadi jauh lebih boros (dua kali lipat).

## 2. Objective
Menemukan penyebab mengapa aplikasi memuat dua URL yang berbeda untuk satu halaman gambar, dan memperbaiki logika pada `Cgbum.kt` (terutama pada fungsi pelarutan gambar / `imageRequest`) agar aplikasi **hanya** melakukan *request* ke URL proxy saja saat fitur tersebut diaktifkan. Hal ini harus berlaku baik saat proses *streaming* (baca langsung) maupun proses *download*, tanpa melakukan *fallback* atau *request* ganda ke URL asli.

## 3. Investigation & Action Steps

### Step 1: Analisis Fungsi `imageRequest` dan `pageListParse`
* Periksa fungsi `imageRequest(page: Page)` di dalam file `Cgbum.kt`. Karena fungsi ini dipakai oleh *image loader* (Coil/Glide) dan juga sistem *downloader* bawaan aplikasi, pastikan modifikasi URL (penambahan `i0.wp.com`) diterapkan dengan benar.
* Pastikan modifikasi tersebut tidak memicu aplikasi untuk menganggapnya sebagai *error* atau memicu mekanisme *bypass* yang berujung me-request ulang URL asli.
* Cek apakah ada masalah *intercept* pada `headers`, *referer*, atau *cache key* yang menyebabkan pemanggilan ganda.

### Step 2: Implementasi Perbaikan
* Sesuaikan logika pembentukan URL agar *reader* dan *downloader* Mihon hanya mendeteksi dan memuat satu *source* gambar. 
* Pastikan URL proxy di-*build* dengan format yang tepat (termasuk *scheme* `https://` yang valid dan penanganan *query quality*) sehingga tidak ditolak oleh *server* atau aplikasi.
* Berikan *code snippet* perbaikan yang spesifik pada fungsi yang bermasalah di `Cgbum.kt`.

### Step 3: Testing & Build Steps (WAJIB)
Setelah kode diperbaiki, lakukan langkah-langkah berikut untuk memastikan ekstensi berjalan dengan baik:
1. Jalankan perintah `./gradlew spotlessApply` di terminal untuk merapikan format kode agar sesuai dengan standar *style guide*.
2. Jalankan perintah `./gradlew testDebugUnitTest` dan pastikan hasilnya **BUILD SUCCESSFUL**.
3. **Test Reader:** *Build* APK ekstensi, jalankan di aplikasi, lalu buka sebuah chapter. Pantau *Network Inspector* (atau log jaringan). Verifikasi bahwa saat fitur WP Proxy aktif, **hanya ada satu request** (ke URL proxy) per halaman gambar.
4. **Test Downloader:** Lakukan proses *download* pada satu chapter komik. Pantau kembali *Network Inspector* untuk memastikan proses unduhan juga **hanya** menarik data dari URL proxy tanpa melakukan *request* ke URL aslinya.
