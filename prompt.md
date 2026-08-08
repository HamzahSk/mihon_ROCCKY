# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Tahap 10 (Stealth Browser, Cookie Manager, dan Cloudflare Interceptor) telah selesai diimplementasikan.

Tugasmu di **Tahap 11** adalah melakukan **Peningkatan UI (UI Polish) & Pembuatan Playground Dinamis**. Halaman Playground harus disesuaikan agar antarmuka eksekusi skrip (*testing UI*) jauh lebih fleksibel: input parameter tidak lagi kaku/statis, melainkan bisa ditambah/disesuaikan dinamis sesuai kebutuhan fungsi skrip, serta dilengkapi tombol untuk menyalin (*copy*) hasil output dalam format JSON maupun Teks/HTML.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md`.
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan tugas terperinci di `ai_memory/task_YYYYMMDD_HHMM_tahap11_ui_improvements_and_dynamic_playground.md` setelah tahap ini selesai.
2. **Build Verification:**
   - Pastikan setiap perubahan dikonfirmasi dengan `./gradlew assembleDebug` tanpa *error* kompilasi.

---

# Bug & UI Requirements Analysis
1. **Playground Input & Button Flexibility:**
   - Saat ini input parameter dan tombol eksekusi (`Run Search`, `Run Detail`) di Playground masih bersifat statis.
   - Dibutuhkan mekanisme di mana pengguna dapat menambah/menghapus baris input parameter (*arguments*) secara dinamis sesuai kebutuhan skrip yang dipanggil.
   - Sediakan pemilih fungsi (*Function Selector* / Dropdown atau Chip) atau tombol dinamis untuk menentukan fungsi mana yang ingin dijalankan di dalam skrip.
2. **Copy Result Functionality:**
   - Pengguna perlu mengambil hasil *parsing* atau *scraping* dengan mudah.
   - Tambahkan tombol **"Copy JSON"** dan **"Copy Raw/HTML Text"** pada *Log/Result Card* menggunakan `ClipboardManager` Android disertai *Toast notification*.
3. **Peningkatan Kualitas UI (Material 3 Refinement):**
   - Rapikan tata letak (*layout*), *padding*, *card elevation*, dan *loading state/progress indicator* di seluruh layar utama (`ScriptsScreen`, `ScriptDetailScreen`, `ImportScriptScreen`, dan `PlaygroundScreen`).

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 11.1: Pembuatan Fleksibilitas Input & Dynamic Buttons di Playground
1. **Dynamic Parameter Inputs State (`PlaygroundViewModel` & `PlaygroundScreen`):**
   - Ubah state parameter tunggal menjadi daftar input dinamis: `List<String>` atau `List<Pair<String, String>>` (Label/Key & Value).
   - Di UI Compose, tampilkan tombol **"+ Add Input/Arg"** dan tombol hapus (**IconButton Delete**) di setiap baris input agar pengguna bisa memasukkan beberapa parameter jika skrip membutuhkannya.
   - Jika skrip tidak membutuhkan input, input field bisa disembunyikan atau dikosongkan.
2. **Dynamic Function Execution:**
   - Buat komponen *Dropdown Menu* atau *Chip Selector* untuk memilih fungsi target di skrip (misal: `search`, `detail`, atau nama fungsi kustom lain yang diketik pengguna).
   - Tambahkan tombol utama eksekusi **"Run Function"** yang secara otomatis meneruskan parameter-parameter tersebut ke `invokeFunction(script, env, functionName, args)`.

### Tahap 11.2: Fitur Copy Output (JSON & Text/HTML)
1. **Action Bar pada Result/Log Card:**
   - Di bagian atas area teks hasil/log (*Result Card*), buat *Action Bar* kecil yang berisi indikator format dan dua tombol aksi:
     - **Tombol "Copy JSON"**: Menyalin hasil keluaran yang telah diformat sebagai JSON (pretty-printed jika memungkinkan).
     - **Tombol "Copy Text/HTML"**: Menyalin teks mentah (*raw string*) atau HTML hasil *fetch/scraping*.
2. **Integrasi ClipboardManager:**
   - Gunakan `LocalClipboardManager.current` di Jetpack Compose untuk menyalin teks ke *clipboard* Android.
   - Tampilkan *Toast* informatif (misal: *"JSON copied to clipboard"*) saat tombol ditekan.

### Tahap 11.3: Polishing UI Seluruh Aplikasi
1. **Peningkatan Komponen UI:**
   - **PlaygroundScreen**: Gunakan `Card` berbatas halus (*OutlinedCard* / *ElevatedCard*), *monospace font* yang nyaman dibaca untuk log result, serta *HorizontalScroll* / *VerticalScroll* yang responsif.
   - **ScriptsScreen & ScriptDetailScreen**: Perbaiki *spacing*, bentuk *chip* status (Active/Inactive), dan konfirmasi saat menghapus skrip.
   - Tampilkan *CircularProgressIndicator* yang rapi saat skrip sedang mengeksekusi proses jaringan.

### Tahap 11.4: Verifikasi & Pembaruan Memori
1. **Build & Test:**
   - Jalankan `./gradlew :app:assembleDebug`.
   - Jalankan unit test (`./gradlew test`) untuk memastikan tidak ada regresi pada logika eksekusi skrip.
2. **Pembaruan Memori:**
   - Catat detail perbaikan UI dan arsitektur Playground dinamis ke `ai_memory/task_YYYYMMDD_HHMM_tahap11_ui_improvements_and_dynamic_playground.md`.
   - Perbarui status di `ai_memory/00_INDEX.md` menjadi **Tahap 11 SELESAI**.

---

**Instruksi Eksekusi:**
Konfirmasi bahwa kamu membaca `memory_prompt.md`. Mulai dari **Tahap 11.1** untuk membuat input & button dinamis di `PlaygroundScreen`, lanjutkan ke **Tahap 11.2** untuk fitur Copy JSON/Text, lakukan uji kompilasi `./gradlew assembleDebug`, dan perbarui riwayat memori setelah selesai.
