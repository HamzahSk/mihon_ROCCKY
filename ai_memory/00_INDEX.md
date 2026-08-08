# MEMORY INDEX — rocat-app

Proyek: `rocat-app` — Android app modular ala Mihon untuk mengelola & menjalankan custom userscript (Rhino engine). Workspace: `rocat-app/` di root repo ini.

## Status Proyek Terkini
- **Tahap 4 SELESAI** (2026-08-08): Stabilisasi crash (root cause: `viewModel()` tanpa factory utk ViewModel ber-constructor default → default factory gagal instansiasi). Fix: `AppViewModelFactory` + registrasi semua ViewModel via Injekt, semua screen pakai `viewModel(factory=...)`. I/O repository dipindah ke `Dispatchers.IO`. Penyempurnaan: validasi import URL (tolak HTML/empty), engine menangkap `EvaluatorException` (syntax error) & `StackOverflowError` tanpa crash.
- Build: `./gradlew :app:assembleDebug` SUCCESS. Unit tests domain + rhino SUCCESS.
- Sebelumnya (Tahap 2 & 3, 2026-08-07): metadata parser, fetch bridge Rhino, storage file-based JSON + CRUD, UI Mihon-style (list/detail/import/playground).
- Baseline Tahap 1: modular gradle, NetworkHelper OkHttp, Injekt DI, RhinoScriptEngine dasar.

## Riwayat Log
| # | Tanggal | File | Ringkasan |
|---|---------|------|-----------|
| 1 | 2026-08-07 | `task_20260807_0440_tahap2_3_script_infrastruktur_dan_ui.md` | Tahap 2 (loader/metadata/fetch/storage) + Tahap 3 (UI management & playground) selesai, build & test hijau. |
| 2 | 2026-08-08 | `task_20260808_0313_tahap4_stabilisasi_dan_perbaikan_script.md` | Tahap 4: fix crash ViewModel factory + I/O Dispatchers.IO, validasi import URL, error handling engine; build & test hijau. |

## Catatan Teknis Penting
- Rhino 1.7.15: support `let/const`, arrow fn, template literal, Promise, generator; TIDAK support `async/await`, spread, optional-chaining, class. Script sample harus ditulis sync (`fetch()` mengembalikan objek Response sync dengan `.text()`/`.json()`).
- `fetch()` bridge: sync, melewati `scriptFetch` (OkHttp client app), balikkan `{status,statusText,ok,url,body,headers,error,text(),json()}`.
- Watchdog: `ScriptContextFactory` + instruction budget 10M; timeout network di `NetworkHelper.newScriptClient()`.
- Parser metadata: blok `==UserScript==` atau fallback `// @tag`; tag `@name/@version/@description/@author/@match/@include/@icon(/iconURL)`.
