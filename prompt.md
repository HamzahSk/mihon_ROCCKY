# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Kita sekarang masuk ke **Tahap 14: Production Readiness (Build, Sign, Splits, & Firebase)**.
Fokus tahap ini adalah pada konfigurasi Gradle untuk mempersiapkan rilis aplikasi `rocat-app`. Kita perlu mengatur *Signing Config*, memecah APK berdasarkan arsitektur CPU (*ABI Splits*), menyiapkan berbagai *Build Types*, serta menyiapkan infrastruktur dasar untuk Firebase (opsional).

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `rocat-app/ai_memory/00_INDEX.md` dan membuat catatan di `rocat-app/ai_memory/task_YYYYMMDD_HHMM_tahap14_production_readiness.md` setelah tahap ini selesai.
2. **Context Path (SANGAT PENTING):**
   - Proyek ini berada di dalam *sub-directory* `rocat-app/`. *Root directory* adalah milik aplikasi lain (Mihon).
   - **SEMUA** modifikasi file (`build.gradle.kts`, `libs.versions.toml`, dll) **WAJIB** dilakukan di dalam folder `rocat-app/`, bukan di *root*.
3. **Safe Build Protocol:**
   - Jangan membuat *build* gagal (*crash*) jika `keystore.properties`, `keystore.jks`, atau `google-services.json` tidak ditemukan. Gunakan blok `try-catch` atau pengecekan `file.exists()` di dalam skrip Kotlin Gradle.
   - Verifikasi akhir wajib menggunakan `cd rocat-app && ./gradlew :app:assembleDebug` dan pastikan sukses.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 14.1: Konfigurasi Signing & Keystore
- Buka file `rocat-app/app/build.gradle.kts`.
- Tambahkan logika Kotlin untuk membaca file `keystore.properties` dari folder `rocat-app/` (jika filenya ada). File ini akan mencari properti `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
- Buat blok `signingConfigs` di dalam blok `android { ... }`.
- Konfigurasikan `release` signing. Jika file properti atau keystore tidak ada, pasang *fallback* menggunakan konfigurasi *debug* (atau biarkan kosong/jangan *throw error*) agar kompilasi di CI/CD atau mesin lokal yang belum di- *setup* tidak putus.

### Tahap 14.2: Konfigurasi ABI Splits (Universal & Spesifik)
- Di dalam blok `android { ... }` pada `rocat-app/app/build.gradle.kts`, tambahkan konfigurasi `splits` untuk memecah APK (ABI).
- Atur `isEnable = true`.
- Masukkan daftar arsitektur standar: `"armeabi-v7a", "arm64-v8a", "x86", "x86_64"`.
- Atur `isUniversalApk = true` agar Gradle tetap menghasilkan satu APK gabungan selain APK per-arsitektur.

### Tahap 14.3: Pengaturan Build Types
- Rombak blok `buildTypes` di `rocat-app/app/build.gradle.kts`.
- **debug**: Konfigurasi standar untuk pengembangan (atur `applicationIdSuffix = ".debug"` agar bisa diinstal berdampingan dengan versi rilis).
- **release**: Aktifkan `isMinifyEnabled = true`, `isShrinkResources = true`, gunakan `signingConfig = signingConfigs.getByName("release")`, dan definisikan Proguard *rules* (misal: `getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"`).
- **preview**: Buat *build type* baru bernama `preview` menggunakan `initWith(getByName("release"))`. Tambahkan `applicationIdSuffix = ".preview"`. Ini berguna untuk menguji versi rilis tanpa menimpa aplikasi *production*.

### Tahap 14.4: Persiapan Integrasi Firebase (Opsional & Aman)
- Buka `rocat-app/gradle/libs.versions.toml`.
- Tambahkan versi dan *library* untuk Firebase BOM (`com.google.firebase:firebase-bom`), Analytics, dan Crashlytics.
- Tambahkan *declaration* plugin Google Services (`com.google.gms.google-services`) dan Crashlytics di bagian `[plugins]`.
- Di `rocat-app/app/build.gradle.kts`, buat logika *conditional plugin application*. Terapkan plugin `com.google.gms.google-services` **hanya** jika file `rocat-app/app/google-services.json` terdeteksi menggunakan `file("google-services.json").exists()`. Jika tidak ada, jangan *apply* pluginnya agar Gradle tidak *error*.

### Tahap 14.5: Verifikasi & Pembaruan Memori
- Jalankan sinkronisasi Gradle pada `rocat-app`.
- Jalankan perintah CLI: `cd rocat-app && ./gradlew :app:assembleDebug` untuk memastikan konfigurasi `build.gradle.kts` yang baru tidak merusak proyek.
- Perbarui status di `rocat-app/ai_memory/00_INDEX.md` menjadi **Tahap 14 SELESAI** dan catat perubahan yang baru saja dilakukan.
