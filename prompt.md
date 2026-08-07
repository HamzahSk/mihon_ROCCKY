# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer. Tahap 1 pembuatan `rocat-app` telah selesai dengan sukses (struktur modular, DI Injekt, Network Layer OkHttp ala Mihon, dan Rhino Scripting Engine dasar sudah terpasang).

Tugasmu sekarang adalah melanjutkan ke **Tahap 2** dan **Tahap 3**, fokus pada infrastruktur penanganan *script* (loader, metadata parsing, network integration) serta UI pengelolaan *script* ala Mihon.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami seluruh protokol manajemen memori, pembatasan token, dan aturan penulisan log secara ketat.
2. **Build Verification:**
   - Pastikan setiap perubahan kode/sub-tahap dikonfirmasi dengan `gradle build` atau `gradle assembleDebug` untuk menjamin tidak ada *error* kompilasi.

# Current Baseline (Hasil Tahap 1)
- Project Gradle modular: `app`, `core:common`, `core:viewmodel`, `domain`, `data`, `scripting:api`, `scripting:rhino`.
- Network Layer: `NetworkHelper`, `Requests.kt`, `OkHttpExtensions`, `JsonUtil`.
- Engine: `RhinoScriptEngine` dengan bridge `fetch()`.
- DI lightweight Injekt bawaan di `AppModule`.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 2: Infrastructure Script Loader & Network Integration
1. **Metadata Parser (Tampermonkey Header Style):**
   - Buat parser di modul `domain`/`data` untuk membaca *header metadata* dari *custom script* JS.
   - Contoh tag yang wajib di-parse: `@name`, `@version`, `@description`, `@author`, `@match` / `@include`, `@icon`.
2. **Enhance Script Engine & Network Integration:**
   - Dalam `RhinoScriptEngine`, sempurnakan *bridge* `fetch()` agar mendukung:
     - Custom HTTP Headers (User-Agent, Referer, Cookie).
     - Response Handling (Status code, Text, JSON response parsing di JS environment).
     - Error/Timeout handling agar script yang menggantung tidak membuat aplikasi crash.
   - Integrasikan `NetworkHelper` Mihon agar semua request dari JS melewati Interceptor/Client yang sama dengan aplikasi (termasuk penanganan CookieJar jika ada).
3. **Local Storage & Storage Repository untuk Script:**
   - Buat mekanisme simpan/muat *script* lokal (misalnya menggunakan database Room atau file-based storage di `data` module).
   - Buat `ScriptRepository` / `ScriptManager` untuk operasi CRUD (Tambah, Baca, Update, Hapus, Toggle Active/Inactive script).

*Lakukan verifikasi build di akhir Tahap 2.*

---

### Tahap 3: UI Management & Execution Flow (Mihon Style)
1. **Extension / Script Management UI (Compose):**
   - Buat/rapikan UI dengan gaya tab *Extensions/Browse* khas Mihon:
     - **Daftar Script Terinstall:** Menampilkan nama, versi, deskripsi, dan *switch* aktif/non-aktif.
     - **Detail Script:** Halaman untuk melihat *code preview*, metadata, dan tombol hapus/edit.
     - **Import/Add Script:** Dialog/Layar untuk menambah *script* baru (via URL, input teks manual, atau pilih file `.js`).
2. **Script Runner / Playground UI (Verifikasi Integrasi):**
   - Buat halaman *Playground/Test Runner* sederhana di mana pengguna bisa memilih salah satu *script* yang aktif, memasukkan URL target, lalu menjalankan fungsi *fetch/extract* dari *script* tersebut dan menampilkan hasil JSON/Text response-nya di UI.
3. **Final Build & Integration Check:**
   - Lakukan kompilasi ulang seluruh project (`./gradlew assembleDebug`).
   - Pastikan tidak ada konflik dependensi, error K2/Kotlin compiler, atau issue pada DI Container.

---
**Instruksi Eksekusi:** Silakan konfirmasi bahwa kamu telah membaca `memory_prompt.md` dan langsung mulai mengeksekusi **Tahap 2**. Laporkan kemajuan dan lakukan tes *build* sebelum berpindah ke Tahap 3.
