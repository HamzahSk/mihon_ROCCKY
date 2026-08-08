# MEMORY INDEX — rocat-app

Proyek: `rocat-app` — Android app modular ala Mihon untuk mengelola & menjalankan custom userscript (Rhino engine). Workspace: `rocat-app/` di root repo ini.

## Status Proyek Terkini
- **Tahap 8 SELESAI** (2026-08-08): Script Execution API + Native DOM Bridge + Testing UI. `JsoupBridge` (baru) diekspos ke Rhino sbg global `RoCatDOM` (parse/select/selectText/selectAttr/selectHtml/has + elemen wrapper text/html/attrs/find/textOf/attrOf/nextElement dll) → skrip tanpa cheerio; `ScriptEngine.invokeFunction()` (+ `ExecuteScript.invoke`) untuk panggil fungsi spesifik (`search`/`detail`) dan stringify hasilnya ke JSON via `NativeJSON`; Playground punya section "Test Execution" (param + tombol Run Search/Run Detail + log area); contoh MangaUpdates ditulis ulang murni sync (fetch sync + RoCatDOM) utk "Load example". Fix bug `BridgeFetch`: `as? String` gagal utk `ConsString` → pakai `Context.toString`. Build `./gradlew :app:assembleDebug` SUCCESS, 22 unit test hijau.
- Sebelumnya (Tahap 7, 2026-08-08): Network & SSL + script loader. `network_security_config.xml` (cleartext + trust-anchor system/user) direferensikan di manifest → fix `CertPathValidatorException`; `NetworkHelper` ala Mihon (UA browser-grade, `MODERN_TLS`/`COMPATIBLE_TLS`, follow redirects, timeout 30s); `ScriptSourceFetcher.normalizeUrl()` (inject `https://`, rewrite GitHub blob→raw, `Dispatchers.IO`); `ImportScriptViewModel` friendly error mapping + example script Rhino-compatible; `WebViewUtil` baru (JS/DOM/database enabled, UA sync). Unit test baru `ScriptSourceFetcherTest`. Build `./gradlew :app:assembleDebug` SUCCESS, semua unit test hijau.
- Sebelumnya (Tahap 6, 2026-08-08): Fix crash Compose `SaveableStateRegistry`. Penyebab (dari crash log): `rememberSaveable { mutableStateListOf(...) }` di `RoCatNav.kt` menampung `SnapshotStateList<String>` yang tidak bisa disimpan ke Bundle. Diperbaiki dengan `saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })`. Build: `./gradlew :app:assembleDebug` SUCCESS.
- Sebelumnya (Tahap 5, 2026-08-08): Global Crash Handler. `CrashHandler` (default uncaught exception handler) menulis report ke `Android/data/app.rocat/files/crash_logs/` via `CrashLogStore`, lalu meluncurkan `CrashActivity` (Activity Compose terpisah, bukan di RoCatNav) yang menampilkan stack trace scrollable + tombol Copy to Clipboard + info path, kemudian `Process.killProcess`. Terdaftar di manifest. Build: `./gradlew :app:assembleDebug` SUCCESS.
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
| 4 | 2026-08-08 | `task_20260808_0507_tahap6_fix_saveable_state_registry.md` | Tahap 6: fix FC crash Compose via custom `listSaver` untuk back stack navigasi di RoCatNav; build hijau. |
| 5 | 2026-08-08 | `task_20260808_0540_tahap7_network_ssl_dan_script_loader.md` | Tahap 7: network_security_config + NetworkHelper TLS/UA ala Mihon, URL normalizer, friendly error import, WebViewUtil modern; build & test hijau. |
| 6 | 2026-08-08 | `task_20260808_1201_tahap8_script_api_dan_ui_execution.md` | Tahap 8: JsoupBridge/RoCatDOM (native DOM), `invokeFunction` (script execution API), Playground "Test Execution" UI, contoh sync MangaUpdates; fix ConsString fetch; build & test hijau. |

## Catatan Teknis Penting
- Network (Tahap 7): `network_security_config.xml` aktif (cleartext + trust system/user); `NetworkHelper.DEFAULT_USER_AGENT` = `Chrome/141.0.0.0`; ConnectionSpec `MODERN_TLS/COMPATIBLE_TLS/CLEARTEXT`; `ScriptSourceFetcher.normalizeUrl()` otomatis inject `https://` & rewrite GitHub blob→raw; `WebViewUtil.setDefaultSettings()` utk engine fallback.
- Rhino 1.7.15: support `let/const`, arrow fn, template literal, Promise, generator; TIDAK support `async/await`, spread, optional-chaining, class. Script sample harus ditulis sync (`fetch()` mengembalikan objek Response sync dengan `.text()`/`.json()`).
- `fetch()` bridge: sync, melewati `scriptFetch` (OkHttp client app), balikkan `{status,statusText,ok,url,body,headers,error,text(),json()}`. Arg URL harus dikonversi `Context.toString` (Rhino `ConsString` bukan `java.lang.String`).
- `RoCatDOM` (Tahap 8): global DOM bridge berbasis Jsoup; `parse/select/selectText/selectAttr/selectHtml/has`; elemen wrapper punya `text,html,innerHtml,attrs,attr(),has(),contains(),find(),textOf(),attrOf(),textsOf(),nextElement()` — semua balik nilai JS native.
- `ScriptEngine.invokeFunction()` (Tahap 8): evaluasi source → panggil fungsi bernama (`search`/`detail`) → hasil `NativeJSON.stringify`. Dipakai Playground "Test Execution" (Run Search/Run Detail).
- Watchdog: `ScriptContextFactory` + instruction budget 10M; timeout network di `NetworkHelper.newScriptClient()`.
- Parser metadata: blok `==UserScript==` atau fallback `// @tag`; tag `@name/@version/@description/@author/@match/@include/@icon(/iconURL)`.
