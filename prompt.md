# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer. Tahap 5 (Global Crash Handler) telah berhasil diimplementasikan. Dari log yang dihasilkan, ditemukan penyebab aplikasi mengalami **Force Close**.

Tugasmu di **Tahap 6** adalah memperbaiki *bug* Jetpack Compose terkait `SaveableStateRegistry` agar aplikasi bisa merender UI dengan sukses tanpa *crash*.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md` untuk memahami protokol manajemen memori secara ketat.
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan tugas setelah tahap ini selesai.
2. **Build Verification:**
   - Pastikan setiap perubahan dikonfirmasi dengan `./gradlew assembleDebug` untuk menjamin tidak ada *error* kompilasi.

# Crash Analysis
Berdasarkan Crash Log terbaru, aplikasi mengalami *crash* saat inisialisasi UI:
`java.lang.IllegalArgumentException: SnapshotStateList(value=[scripts])@221679637 cannot be saved using the current SaveableStateRegistry. The default implementation only supports types which can be stored inside the Bundle.`

Penyebabnya adalah penggunaan `rememberSaveable` pada *state* yang berisi *list of custom objects* (kemungkinan besar `Script`), di mana objek tersebut tidak mengimplementasikan `Parcelable` atau tidak memiliki custom `Saver`.

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 6.1: Perbaikan State UI (Bug Fix)
1. **Identifikasi Masalah:**
   - Cari penggunaan `rememberSaveable` yang menampung *list* (seperti `mutableStateListOf` atau menyimpan *list* dari *state* ViewModel) di komponen UI (kemungkinan di `ScriptsScreen.kt`, `PlaygroundScreen.kt`, atau navigasinya).
2. **Implementasi Solusi (Pilih yang Paling Sesuai):**
   - **Opsi A (Rekomendasi):** Jika data *scripts* sudah disimpan dan diatur oleh `ViewModel` (di-*hoist*), UI cukup mengobservasinya via `collectAsState()`. Jika butuh *local state* yang tidak perlu *survive process death*, ubah `rememberSaveable` menjadi `remember`.
   - **Opsi B:** Jika *state* tersebut mutlak harus *survive process death*, buat kelas `Script` menjadi `@Parcelize` (pastikan *plugin* `kotlin-parcelize` sudah aktif) atau buat custom `Saver`.

### Tahap 6.2: Verifikasi
1. **Build Ulang:**
   - Jalankan `./gradlew assembleDebug`.
   - Pastikan kode bisa dikompilasi dengan sukses setelah menghapus atau memperbaiki `rememberSaveable` tersebut.

**Instruksi Eksekusi:** Konfirmasi bahwa kamu membaca `memory_prompt.md`. Mulai dari Tahap 6.1, temukan lokasi spesifik `rememberSaveable` yang bermasalah, jelaskan perbaikan yang kamu lakukan, lalu lakukan verifikasi *build* sebelum mencatatnya di memori.
