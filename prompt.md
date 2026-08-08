# Role and Objective
Kamu adalah AI Software Engineer dan Android Developer handal. Tahap 8 (Script Execution API & UI) telah berhasil diselesaikan, namun ditemukan *bug* fatal saat skrip dijalankan di *device*/emulator Android.

Tugasmu di **Tahap 9** adalah memperbaiki *bug* Rhino engine terkait kompilasi *bytecode* di Android, serta merapikan penanganan tampilan *error* di antarmuka `PlaygroundScreen`.

# Memory and Constraints (CRITICAL)
1. **BACA ATURAN MEMORI:**
   - Buka dan baca file `memory_prompt.md`.
   - Wajib memperbarui log di `ai_memory/00_INDEX.md` dan membuat catatan tugas terperinci di `ai_memory/task_YYYYMMDD_HHMM_tahap9_fix_rhino_class_loader.md` setelah tahap ini selesai.
2. **Build Verification:**
   - Pastikan kode dapat dikompilasi sukses dengan `./gradlew assembleDebug`.

---

# Bug Analysis & Root Cause
1. **Error Log di UI Playground:** Muncul pesan merah bertuliskan `can't load this type of class file` saat tombol **Run** atau **Run Search** ditekan.
2. **Penyebab (Rhino on Android Issue):** - Rhino secara otomatis mencoba mengompilasi JavaScript ke Java Bytecode standar (`.class`) untuk mempercepat eksekusi (Optimization Level > 0).
   - Android menggunakan Dalvik/ART (DEX bytecode) dan *classloader*-nya tidak bisa memuat file `.class` Java standar secara *on-the-fly* (dinamis), sehingga pelemparan `IllegalArgumentException` atau `UnsupportedOperationException` terjadi.
3. **Solusi Mutlak:** - Rhino harus dipaksa berjalan pada **Interpretation Mode** (Mode Interpretasi murni) dengan menyetel `optimizationLevel = -1` pada `Context` miliknya.

---

# Execution Plan (Kerjakan Secara Bertahap)

### Tahap 9.1: Perbaikan Konteks Rhino (Core Fix)
1. **Modifikasi `ScriptContextFactory` atau Inisialisasi Rhino:**
   - Temukan lokasi di mana `Context` Rhino dikonfigurasi (biasanya di `core/scripting/rhino/ScriptContextFactory.kt` atau di dalam `RhinoScriptEngine`).
   - Pastikan metode inisialisasi konteks (seperti `makeContext` atau `onContextCreated`) memuat baris berikut:
     ```kotlin
     context.optimizationLevel = -1
     ```
   - Pastikan ini diterapkan untuk **semua** eksekusi skrip (baik `execute` biasa maupun `invokeFunction`).

### Tahap 9.2: Peningkatan Error Handling di Playground UI
1. **Perbaikan Penangkapan Error (ViewModel & UI):**
   - Saat ini *error* merusak UI dengan teks merah yang tiba-tiba muncul di luar kotak *Result*.
   - Ubah logika di `PlaygroundViewModel` atau penangkapan state di UI. Jika terjadi *exception* saat `Run` atau `Run Search/Detail`, masukkan pesan *error* tersebut **ke dalam** area teks "Result" atau area log JSON yang berlatar abu-abu, bukan mencetaknya sebagai teks melayang.
   - Tangkap exception yang lebih luas (termasuk `Exception` atau `Throwable` murni) selama eksekusi di blok `try-catch`, agar aplikasi tidak *force close* jika ada runtime error dari JS.

### Tahap 9.3: Verifikasi & Pembaruan Memori
1. **Build & Test:**
   - Jalankan `./gradlew :app:assembleDebug`.
   - Pastikan unit test domain dan rhino (seperti `RhinoScriptEngineTest`) masih lulus tanpa *error*.
2. **Pembaruan Memori:**
   - Catat detail perbaikan optimasi `-1` Rhino ke `ai_memory/task_YYYYMMDD_HHMM_tahap9_fix_rhino_class_loader.md`.
   - Perbarui status di `ai_memory/00_INDEX.md` menjadi **Tahap 9 SELESAI**.

---

**Instruksi Eksekusi:**
Konfirmasi bahwa kamu membaca `memory_prompt.md`. Mulai dari **Tahap 9.1** dengan mencari letak pembuatan `Context` Rhino dan set `optimizationLevel = -1`. Lanjutkan merapikan *state error* di UI Playground pada **Tahap 9.2**, lakukan kompilasi, dan catat hasilnya ke memori.
