# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Tahap 6 (perbaikan crash `SaveableStateRegistry` pada Jetpack Compose) telah selesai diimplementasikan.

Tugasmu di **Tahap 7** adalah memperbaiki masalah koneksi jaringan (*networking*) dan SSL handshake (`CertPathValidatorException`), memastikan implementasi OkHttp & Network Helper stabil, mengintegrasikan konfigurasi WebView modern, serta menyempurnakan fitur *Fetch & Import Script*.

**PENTING:** Gunakan modul/kode sumber aplikasi **Mihon di direktori utama** sebagai acuan standar (*source of truth*) untuk implementasi `NetworkHelper`, interceptor, konfigurasi OkHttp, `network_security_config`, dan penanganan WebView.

---

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami protokol manajemen memori secara ketat.
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan tugas terperinci di `ai_memory/task_YYYYMMDD_HHMM_tahap7_network_ssl_dan_script_loader.md` setelah tahap ini selesai.
2. **Kepatuhan Arsitektur Mihon:**
   - Telusuri implementasi jaringan pada kode Mihon di root/direktori utama (khususnya implementasi `NetworkHelper`, `UserAgentInterceptor`, `OkHttpClient`, dan utilitas WebView).
   - Terapkan pola serupa ke dalam arsitektur `rocat-app`.
3. **Build & Test Verification:**
   - Pastikan setiap perubahan dikonfirmasi dengan `./gradlew assembleDebug` dan unit test terkait lulus tanpa *error*.

---

# Bug & Issue Analysis
1. **SSL / Trust Anchor Error:**
   - Terjadi error: `java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.` saat melakukan *fetch* URL HTTPS (misalnya `https://google.com` atau link raw script).
   - **Akar Masalah:**
     - `NetworkSecurityConfig` belum dikonfigurasi atau membatasi CA trust anchor.
     - `OkHttpClient` belum dikonfigurasi dengan `ConnectionSpec` yang tepat (TLS 1.2/1.3), atau `TrustManager` sistem Android tidak terpasang dengan benar.
     - Belum ada `User-Agent` interceptor standar sehingga request di-reject oleh server/WAF.
2. **Script Import & Load Issues:**
   - Penanganan error di `AddScriptViewModel` masih melempar raw exception ke UI tanpa fallback atau format URL otomatis (misal: penanganan otomatis link raw GitHub/Gist).
   - Tombol "Load example" dan textarea source butuh template default yang valid dan kompatibel dengan parser script.
3. **WebView Modern Support:**
   - Pastikan utilitas WebView selaras dengan standar Mihon (DOM storage, JS enabled, database, user agent konsisten) untuk kebutuhan rendering atau engine fallback.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 7.1: Analisis Referensi Mihon & Konfigurasi Network Security
1. **Pelajari Network Layer Mihon:**
   - Buka dan pelajari implementasi `NetworkHelper.kt`, `UserAgentInterceptor.kt`, dan `AndroidNetworkSecurityConfig` dari aplikasi Mihon di direktori utama.
2. **Network Security Config:**
   - Periksa dan buat/sesuaikan `app/src/main/res/xml/network_security_config.xml`:
     ```xml
     <?xml version="1.0" encoding="utf-8"?>
     <network-security-config>
         <base-config cleartextTrafficPermitted="true">
             <trust-anchors>
                 <certificates src="system" />
                 <certificates src="user" />
             </trust-anchors>
         </base-config>
     </network-security-config>
     ```
   - Pastikan `AndroidManifest.xml` mereferensikan konfigurasi tersebut (`android:networkSecurityConfig="@xml/network_security_config"`).

### Tahap 7.2: Perbaikan & Standardisasi OkHttp (`NetworkHelper`)
1. **Interceptor & TLS Builder ala Mihon:**
   - Perbarui `NetworkHelper` di `rocat-app`:
     - Tambahkan `User-Agent` interceptor (menggunakan User-Agent browser/Mihon standar).
     - Atur `ConnectionSpec.MODERN_TLS` dan `ConnectionSpec.COMPATIBLE_TLS`.
     - Konfigurasi `followRedirects(true)` dan `followSslRedirects(true)`.
     - Atur timeouts (Connect, Read, Write) yang wajar (15–30 detik).
2. **Script Fetch Client:**
   - Pastikan client yang dipakai untuk `scriptFetch` dan `AddScriptViewModel` menggunakan instance `NetworkHelper.client` yang telah diperbaiki.

### Tahap 7.3: Penyempurnaan Script Import & Load Example
1. **URL Normalizer & Fetcher:**
   - Bersihkan input URL (trim whitespace, pastikan scheme `https://` terisi jika user hanya mengetik domain).
   - Jalankan request secara aman di `Dispatchers.IO` dengan pemetaan pesan error yang mudah dipahami (misal: format URL salah, koneksi terputus, atau SSL error).
2. **Load Example & Parser:**
   - Pastikan fungsi "Load example" menyediakan skrip demo yang kompatibel dengan Rhino engine dan memiliki header `==UserScript==` lengkap.
   - Pastikan alur simpan ke `ScriptRepository` berjalan lancar setelah download berhasil.

### Tahap 7.4: Konfigurasi WebView Modern
1. **WebSettings & WebViewUtil:**
   - Terapkan konfigurasi WebView ala Mihon:
     - `settings.javaScriptEnabled = true`
     - `settings.domStorageEnabled = true`
     - `settings.databaseEnabled = true`
     - Sinkronisasi `userAgentString` dengan `NetworkHelper`.

### Tahap 7.5: Verifikasi & Pembaruan Memori
1. **Build Ulang & Test:**
   - Jalankan `./gradlew :app:assembleDebug`.
   - Jalankan `./gradlew test` untuk memastikan semua unit test hijau.
2. **Pembaruan Memori:**
   - Buat log tugas baru: `ai_memory/task_YYYYMMDD_HHMM_tahap7_network_ssl_dan_script_loader.md`.
   - Perbarui status di `ai_memory/00_INDEX.md` menjadi **Tahap 7 SELESAI**.

---

**Instruksi Eksekusi:**
Konfirmasi bahwa kamu telah membaca `memory_prompt.md` dan `00_INDEX.md`. Mulai dari **Tahap 7.1**, periksa referensi Mihon di direktori utama, terapkan konfigurasi `network_security_config` serta `NetworkHelper`, verifikasi dengan build `./gradlew assembleDebug`, dan perbarui memori.
