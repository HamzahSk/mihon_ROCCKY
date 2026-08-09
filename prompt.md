# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Kita sekarang masuk ke **Tahap 20: Native Base64 Bridge, Custom User-Agent, & Custom DNS Settings**.
Fokus tahap ini adalah memindahkan logika `decodeBase64` dari *scraper* JS ke *native* Kotlin agar lebih efisien, serta menambahkan pengaturan Jaringan (Network) di aplikasi yang mencakup kustomisasi *User-Agent* dan opsi DNS Over HTTPS (DoH).

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan di `ai_memory/task_YYYYMMDD_HHMM_tahap20_network_settings.md` setelah tahap ini selesai.
2. **Context Path:**
   - Semua modifikasi file wajib dilakukan di dalam folder `rocat-app/`.
3. **Jetpack Compose & Modern Android:**
   - Gunakan `SettingsRepository` (DataStore/SharedPreferences) untuk menyimpan konfigurasi *User-Agent* dan DNS.
   - Gunakan `okhttp3-dnsoverhttps` (jika perlu) atau implementasi `Dns` OkHttp untuk mengatur *custom* DNS.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 20.1: Pembuatan Native Base64 Bridge
- **Update Bridge:** Tambahkan fungsi baru `decodeBase64(input: String): String` pada antarmuka bridge global (misalnya di `ScriptUiBridge` atau buat `RoCatUtils` baru).
- **Implementasi Native:** Gunakan `android.util.Base64.decode(input, Base64.DEFAULT)` di Kotlin, lalu ubah hasilnya menjadi `String` (UTF-8). Tangani *padding* error dengan aman (gunakan blok `try-catch` dan kembalikan *string* kosong jika gagal).
- **Update JS Scraper:** Ubah file `scrape_anichin.js` agar menggunakan pemanggilan *native* ini (misalnya `RoCatUI.decodeBase64(str)`) alih-alih melakukan *decode* murni menggunakan fungsi JS.

### Tahap 20.2: UI Pengaturan Jaringan (Network Settings)
- **SettingsScreen:** Tambahkan kategori baru "Jaringan" di `SettingsScreen`.
- **Custom User-Agent:** Buat *input text* (TextField) untuk mengubah *User-Agent*. Jika kosong, gunakan *default* (misalnya "Chrome/143.0...").
- **DNS Configuration:** Buat *dropdown* atau opsi *radio button* untuk memilih DNS. Opsi yang tersedia:
  - System Default (Bawaan)
  - Cloudflare (1.1.1.1)
  - Google (8.8.8.8)
  - Quad9 (9.9.9.9)
  - Custom DNS (tampilkan TextField tambahan jika ini dipilih, untuk memasukkan URL DoH).

### Tahap 20.3: Integrasi Data Store & OkHttp (NetworkHelper)
- **State Management:** Simpan preferensi *User-Agent* dan setelan DNS di `SettingsRepository`.
- **NetworkHelper:** Modifikasi `NetworkHelper` agar membaca preferensi ini saat membuat `OkHttpClient`.
  - Jika *User-Agent* diubah, pastikan `StealthHeadersInterceptor` atau interceptor terkait menggunakan nilai baru tersebut.
  - Jika DNS diubah dari sistem *default*, konfigurasikan `OkHttpClient.Builder().dns(...)` atau gunakan modul DoH OkHttp dengan URL yang sesuai (misal: `https://cloudflare-dns.com/dns-query`).

### Tahap 20.4: Pengujian & Validasi
- Jalankan aplikasi dan pastikan perubahan DNS serta *User-Agent* tersimpan dan bertahan meski aplikasi di-*restart*.
- Lakukan eksekusi *script* Anichin dan pastikan `decodeBase64` *native* berjalan dengan lancar saat melakukan *scraping stream* HLS.
- Pastikan build tidak bermasalah: jalankan `./gradlew :app:assembleDebug` dan unit test terkait.

### Tahap 20.5: Update Memory
- Perbarui file `00_INDEX.md` dengan status **Tahap 20 SELESAI**.
- Buat catatan di `task_YYYYMMDD_HHMM_tahap20_network_settings.md`.
