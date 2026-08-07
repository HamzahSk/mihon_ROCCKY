# MEMORY INDEX — rocat-app

Proyek: `rocat-app` — Android app modular ala Mihon untuk mengelola & menjalankan custom userscript (Rhino engine). Workspace: `rocat-app/` di root repo ini.

## Status Proyek Terkini
- **Tahap 2 & 3 SELESAI** (2026-08-07): Metadata parser (Tampermonkey header), fetch bridge Rhino yang ditingkatkan (headers, status, JSON, error/timeout, watchdog instruction budget), storage script file-based JSON + CRUD repository, UI Mihon-style (Scripts list, Detail + edit/delete, Import via URL/text, Playground test-runner).
- Build: `./gradlew :app:assembleDebug` SUCCESS. Unit tests: 14 passing (9 engine Rhino, 5 metadata parser).
- Baseline Tahap 1 (sebelum sesi ini): modular gradle (app, core:common, core:viewmodel, domain, data, scripting:api, scripting:rhino), NetworkHelper OkHttp, Injekt DI, RhinoScriptEngine dasar.

## Riwayat Log
| # | Tanggal | File | Ringkasan |
|---|---------|------|-----------|
| 1 | 2026-08-07 | `task_20260807_0440_tahap2_3_script_infrastruktur_dan_ui.md` | Tahap 2 (loader/metadata/fetch/storage) + Tahap 3 (UI management & playground) selesai, build & test hijau. |

## Catatan Teknis Penting
- Rhino 1.7.15: support `let/const`, arrow fn, template literal, Promise, generator; TIDAK support `async/await`, spread, optional-chaining, class. Script sample harus ditulis sync (`fetch()` mengembalikan objek Response sync dengan `.text()`/`.json()`).
- `fetch()` bridge: sync, melewati `scriptFetch` (OkHttp client app), balikkan `{status,statusText,ok,url,body,headers,error,text(),json()}`.
- Watchdog: `ScriptContextFactory` + instruction budget 10M; timeout network di `NetworkHelper.newScriptClient()`.
- Parser metadata: blok `==UserScript==` atau fallback `// @tag`; tag `@name/@version/@description/@author/@match/@include/@icon(/iconURL)`.
