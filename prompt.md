# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Kita sekarang masuk ke **Tahap 17: Perbaikan Bug First-Launch, Storage Skrip, dan UI Kategori (Collapsible)**.
Fokus tahap ini adalah memperbaiki *bug* reaktivitas saat pengguna pertama kali mengatur *storage*, memastikan skrip yang diimpor langsung disimpan sebagai file fisik (`.js`) di *storage*, menambahkan menu *long-press* pada daftar skrip, dan membuat sistem kategori skrip yang bisa dilipat (berbasis metadata) agar UI lebih rapi.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan di `ai_memory/task_YYYYMMDD_HHMM_tahap17_ui_and_storage_fixes.md` setelah tahap ini selesai.
2. **Context Path (SANGAT PENTING):**
   - Proyek ini berada di dalam *sub-directory* `rocat-app/`.
   - **SEMUA** modifikasi file **WAJIB** dilakukan di dalam folder `rocat-app/`.
3. **Jetpack Compose & Modern Android:**
   - Gunakan `combinedClickable` untuk *long-press*.
   - Buat *state* menjadi reaktif menggunakan `StateFlow` atau `collectAsState`.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 17.1: Perbaikan Bug First-Launch (Storage Setup Stuck)
- **Masalah:** Saat instalasi baru, setelah pengguna memilih folder di `StorageSetupScreen`, aplikasi *stuck* dan tidak masuk ke menu utama (harus di-*restart* dulu).
- **Akar Masalah:** Di `RoCatNav.kt`, pengecekan `if (!storageManager.isConfigured)` tidak reaktif, sehingga Compose tidak melakukan *re-compose* saat izin *storage* berhasil didapatkan.
- **Solusi:** Ubah `isConfigured` di `StorageManager` menjadi `StateFlow<Boolean>` (atau sediakan *flow* observasi dari `SettingsRepository`). Di `RoCatApp()`, gunakan `collectAsState()` untuk memantau nilai ini, sehingga saat berubah menjadi `true`, UI otomatis berpindah ke `RoCatAppNav()`.

### Tahap 17.2: Simpan Fisik Skrip ke Storage Saat Import
- **Masalah:** Skrip yang baru ditambahkan hanya masuk ke JSON/Database internal, belum dibuatkan file fisiknya di direktori SAF.
- **Solusi:** Modifikasi alur *import* skrip (misal di `ImportScriptViewModel`). Saat skrip berhasil diimpor, gunakan `StorageManager` untuk membuat sub-folder baru di dalam direktori utama (misal: `[Utama]/Scripts/[Script_ID]/`).
- Simpan *source code* skrip ke dalam sub-folder tersebut sebagai file berekstensi `.js` (atau `.txt`). Gunakan utilitas penulisan file menggunakan `ContentResolver.openOutputStream` seperti yang diterapkan di Tahap 16.

### Tahap 17.3: Menu Aksi Tahan Lama (Long Press) pada Skrip
- Buka file `ScriptsScreen.kt`.
- Tambahkan `Modifier.combinedClickable` (pastikan menggunakan `ExperimentalFoundationApi` jika diperlukan) pada kartu/item skrip.
- **Aksi:** - *Click* (klik biasa): Buka Canvas (seperti biasa).
  - *Long Click* (tahan lama): Tampilkan `ModalBottomSheet` atau `DropdownMenu` (Dialog) yang berisi opsi: **Edit** dan **Hapus (Delete)**.
- Hubungkan opsi "Hapus" ke fungsi penghapusan yang sudah ada, dan "Edit" ke halaman detail atau editor.

### Tahap 17.4: UI Kategori Skrip (Collapsible/Akordion)
- **Metadata:** Tambahkan dukungan *parsing* metadata kategori (misal membaca `// @category` atau `// @group` dari *header* skrip). Jika tidak ada, masukkan ke kategori "Lainnya" atau "Default".
- **UI:** Di `ScriptsScreen`, kelompokkan daftar skrip berdasarkan kategorinya (`Map<String, List<Script>>`).
- Gunakan `LazyColumn` dan ubah tampilannya menjadi kategori yang bisa dilipat (*collapsible*).
- Tampilkan nama kategori sebagai *header* (tebal/rapi). Jika *header* diklik, daftar skrip di bawahnya akan menyusut (terlipat) atau memuai. Simpan status *expand/collapse* ini menggunakan `remember { mutableStateMapOf<String, Boolean>() }` agar tiap kategori bisa dikontrol secara independen.

### Tahap 17.5: Verifikasi Build & Update Memory
- Jalankan `cd rocat-app && ./gradlew :app:assembleDebug` untuk memastikan semua perbaikan reaktivitas dan UI berjalan mulus tanpa *error*.
- Perbarui file `00_INDEX.md` dengan status **Tahap 17 SELESAI** dan rangkum perubahan teknisnya.
