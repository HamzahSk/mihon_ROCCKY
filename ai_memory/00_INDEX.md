# MEMORY INDEX — rocat-app

Proyek: `rocat-app` — Android app modular ala Mihon untuk mengelola & menjalankan custom userscript (Rhino engine). Workspace: `rocat-app/` di root repo ini.

## Status Proyek Terkini
- **Tahap 5 SELESAI** (2026-08-08): Global Crash Handler. `CrashHandler` (default uncaught exception handler) menulis report ke `Android/data/app.rocat/files/crash_logs/` via `CrashLogStore`, lalu meluncurkan `CrashActivity` (Activity Compose terpisah, bukan di RoCatNav) yang menampilkan stack trace scrollable + tombol Copy to Clipboard + info path, kemudian `Process.killProcess`. Terdaftar di manifest. Build: `./gradlew :app:assembleDebug` SUCCESS.
- Build: `./gradlew :app:assembleDebug` SUCCESS. Unit tests domain + rhino SUCCESS.
- Sebelumnya (Tahap 4, 2026-08-08): Stabilisasi crash ViewModel factory (`AppViewModelFactory` + Injekt), I/O `Dispatchers.IO`, validasi import URL, error handling Rhino engine.
- Sebelumnya (Tahap 2 & 3, 2026-08-07): metadata parser, fetch bridge Rhino, storage file-based JSON + CRUD, UI Mihon-style (list/detail/import/playground).
- Baseline Tahap 1: modular gradle, NetworkHelper OkHttp, Injekt DI, RhinoScriptEngine dasar.

## Riwayat Log
| # | Tanggal | File | Ringkasan |
|---|---------|------|-----------|
| 1 | 2026-08-07 | `task_20260807_0440_tahap2_3_script_infrastruktur_dan_ui.md` | Tahap 2 (loader/metadata/fetch/storage) + Tahap 3 (UI management & playground) selesai, build & test hijau. |
| 2 | 2026-08-08 | `task_20260808_0313_tahap4_stabilisasi_dan_perbaikan_script.md` | Tahap 4: fix crash ViewModel factory + I/O Dispatchers.IO, validasi import URL, error handling engine; build & test hijau. |
| 3 | 2026-08-08 | `task_20260808_0436_tahap5_global_crash_handler.md` | Tahap 5: global crash handler + CrashLogStore (Android/data) + CrashActivity (stack trace scrollable, copy, path info); build hijau. |

## Catatan Teknis Penting
- Rhino 1.7.15: support `let/const`, arrow fn, template literal, Promise, generator; TIDAK support `async/await`, spread, optional-chaining, class. Script sample harus ditulis sync (`fetch()` mengembalikan objek Response sync dengan `.text()`/`.json()`).
- `fetch()` bridge: sync, melewati `scriptFetch` (OkHttp client app), balikkan `{status,statusText,ok,url,body,headers,error,text(),json()}`.
- Watchdog: `ScriptContextFactory` + instruction budget 10M; timeout network di `NetworkHelper.newScriptClient()`.
- Parser metadata: blok `==UserScript==` atau fallback `// @tag`; tag `@name/@version/@description/@author/@match/@include/@icon(/iconURL)`.
