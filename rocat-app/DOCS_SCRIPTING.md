# RoCat Scripting API — Dokumentasi Resmi

Panduan lengkap untuk membuat skrip _scraper_ (userscript) untuk aplikasi **RoCat**.
Dokumen ini mencerminkan API yang dibangun di tahap-tahap pengembangan sebelumnya:
`ScriptUiBridge` (global `RoCatUI`), `JsoupBridge` (global `RoCatDOM`), `fetch()`
sinkron berbasis OkHttp, dan integrasi Media3/ExoPlayer untuk pemutaran HLS.

---

## Ringkasan Eksekusi & Linguistik

Skrip RoCat adalah **JavaScript murni** yang dijalankan oleh mesin **Rhino 1.7.15**
dalam mode interpretasi. Dua konsekuensi penting:

| Hal | Fakta |
|-----|-------|
| `async` / `await` | **TIDAK didukung.** Tulis semua alur secara sinkron. |
| `import`/`export` | Tidak didukung — tidak ada bundler. Semua kode dalam satu file. |
| `class`, spread `...`, optional chaining `?.` | Tidak didukung oleh Rhino 1.7.15. |
| `let`, `const`, arrow function, template literal, generator, `Promise` | Didukung. |
| Watchdog | Setiap run punya batas instruksi (10.000.000) — `while(true)` akan di-hentikan. |
| Anti-crash | Setiap error/throw ditangkap Kotlin dan ditampilkan sebagai output, **tidak pernah menutup app**. |

Global yang tersedia di setiap skrip:

- `fetch(url, options)` — HTTP sinkron berbasis OkHttp (Response `.text()`/`.json()`).
- `RoCatUI` — bridge UI Compose (hanya tersedia saat skrip dibuka di **Canvas**).
- `RoCatDOM` — bridge parsing HTML berbasis Jsoup (pengganti Cheerio).
- `JSON`, `Math`, `String`, `encodeURIComponent`, dll. — standar JS.

---

## 1. Struktur Dasar Skrip & Metadata

### 1.1 Blok Metadata Wajib

Tiap skrip **dianjurkan** membuka file dengan blok `==UserScript==` (gaya
Tampermonkey/Greasemonkey). Parser `ScriptMetadataParser` membaca blok ini, dan
bila blok tidak ada ia *fallback* ke pemindaian setiap baris `// @tag value`.

```javascript
// ==UserScript==
// @name         Anichin Scraper
// @version      2.0.0
// @description  Cari, baca detail, dan streaming HLS (.m3u8) anime dari Anichin
//               via RoCatUI.addVideo (baris lanjutan @description digabung newline).
// @author       RoCat AI
// @category     Anime
// @icon         https://anichin.cafe/path/icon.png
// @match        https://anichin.cafe/*
// @match        https://anichin.stream/*
// @include      https://*.anichin.cafe/*
// ==/UserScript==
```

### 1.2 Tag yang Didukung

| Tag | Arti | Keterangan |
|-----|------|-----------|
| `@name` | Nama skrip | Menjadi judul kartu & top bar Canvas. **Wajib diisi.** |
| `@version` | Versi | Bentuk semver (contoh `1.0.0`). Default `0.0.0`. |
| `@description` | Deskripsi | Mendukung **multi-baris** (`//` lanjutan digabung newline). |
| `@author` | Pembuat | Mendukung multi-baris. |
| `@icon` | URL cover ikon | `@iconURL` juga diterima sebagai alias. |
| `@category` | Label kategorisasi | Dipakai untuk mengelompokkan skrip di daftar (fallback `@group`). Kosong → grup "Others". |
| `@match` | Daftar pola URL | Digabung dengan `@include` menjadi allow-list (informational). |
| `@include` | Daftar pola URL | Alias `@match`. |
| `@grant` | Izin | `none` dirender sebagaimana adanya; saat ini tidak dipakai untuk gating API. |

Semua tag bersifat case-*insensitive*. `@name` adalah satu-satunya nilai wajib yang
memengaruhi tampilan; jika tidak ada, aplikasi memakai nama file/generated id.

### 1.3 Siklus Hidup Skrip (Script Lifecycle)

Skrip dijalankan di **Canvas** (kanvas per-skrip). Alur hidupnya:

1. Pengguna mengimpor / memilih skrip → layar **Script Canvas** terbuka.
2. Aplikasi membersihkan kanvas lalu **secara otomatis memanggil `onLaunch()`**
   setiap kali kanvas dibuka atau source skrip berubah (kode diedit).
3. `onLaunch()` menggambar antarmuka awal lewat `RoCatUI.*` (input, tombol, grid…).
4. Interaksi selanjutnya **dikendalikan sepenuhnya oleh JavaScript**:
   - Menekan tombol → fungsi bernama dipanggil dengan **objek input** (`{ id: value }`).
   - Menekan tile grid → fungsi bernama dipanggil dengan **payload JSON string**.
   - Skrip "berpindah halaman" dengan memanggil `RoCatUI.clear()` lalu menggambar ulang.
5. Skrip tanpa `onLaunch()` tidak *canvas-driven* — tidak terjadi apa-apa di kanvas
   (tidak error). Skrip seperti itu bisa dijalankan lewat entry point `main()` (jika ada).

Contoh pola navigasi (Search → Grid → Detail):

```javascript
function onLaunch() {
    RoCatUI.clear();
    RoCatUI.addInput("query", "Cari anime...");
    RoCatUI.addButton("Cari", "doSearch");
}

function doSearch(inputs) {
    // inputs.query -> teks yang diketik user
    RoCatUI.clear();
    RoCatUI.addButton("← Kembali", "onLaunch");
    // ... fetch + parse ...
    RoCatUI.addGrid(3, JSON.stringify(results), "openDetail");
}

function openDetail(itemJsonString) {
    var item = JSON.parse(itemJsonString); // objek asli item grid
    RoCatUI.clear();
    RoCatUI.addButton("← Kembali", "onLaunch");
    // buka halaman detail item...
}
```

---

## 2. Dokumentasi UI Bridge — global `RoCatUI`

Objek global `RoCatUI` tersedia saat skrip dijalankan di **Script Canvas**
(bridge `ScriptUiBridge` aktif). Semua panggilan *thread-safe*: hasil dimarshal ke
main thread oleh aplikasi, dan **kegagalan di dalam bridge tidak pernah menghentikan
skrip** (exception ditelan dan skrip lanjut).

### 2.1 Input & Interaksi

#### `RoCatUI.addInput(id, hint)`
Menambahkan satu kolom input teks yang diidentifikasikan oleh `id`.

| Parameter | Tipe | Deskripsi |
|-----------|------|-----------|
| `id` | `string` | Kunci unik; dipakai sebagai nama properti objek input. |
| `hint` | `string` | Placeholder yang ditampilkan di kolom. |

Jika skrip memanggil `addInput` berulang untuk `id` sama, nilai input lama **dipertahankan**
dan hanya hint yang disegarkan. Saat tombol ditekan, semua input yang **tidak kosong**
dikumpulkan menjadi satu objek `{ id: value }`.

#### `RoCatUI.addButton(label, functionName)`
Menambahkan tombol. Saat ditekan, aplikasi memanggil fungsi bernama `functionName`
dan meneruskan semua input sebagai **satu argumen objek**.

```javascript
RoCatUI.addInput("video_url", "Tempel URL video...");
RoCatUI.addButton("Ekstrak", "onExtract");

// definisi handler → menerima objek input
function onExtract(inputs) {
    var url = inputs.video_url;
    // ...
}
```

> Fungsi target yang tidak ada di skrip → output `Script has no function named '...'`.
> Return `undefined`/`null` dibulatkan menjadi string kosong (tidak muncul di console).

### 2.2 Media & Pratinjau

#### `RoCatUI.addImage(url, title, allowDownload)`
Menampilkan kartu pratinjau gambar (dimuat dengan Coil).

| Parameter | Tipe | Default | Deskripsi |
|-----------|------|---------|-----------|
| `url` | `string` | — | URL gambar. |
| `title` | `string` | `""` | Judul tampil di atas gambar. |
| `allowDownload` | `boolean` | `true` | `true` → tampilkan tombol "simpan ke folder scrape". |

Contoh penutup sampul di halaman detail:

```javascript
RoCatUI.addImage(coverUrl, title, true);   // dengan tombol download
RoCatUI.addImage(coverUrl, title, false);  // tanpa download
```

#### `RoCatUI.addVideo(url, title, isStreamHls, allowDownload)`
Menampilkan kartu video dengan **pemutar inline Media3/ExoPlayer native** + tombol
download dan toggle full screen.

| Parameter | Tipe | Default | Deskripsi |
|-----------|------|---------|-----------|
| `url` | `string` | — | URL sumber video (progressive MP4/WebM atau `.m3u8`). |
| `title` | `string` | `""` | Judul di kartu. |
| `isStreamHls` | `boolean` | `false` | `true` → dikonfigurasi sebagai **HLS media source**. |
| `allowDownload` | `boolean` | `true` | Tampilkan/sembunyikan tombol unduh. |

**Penting — HLS otomatis memakai ExoPlayer native.** Pemutar memilih
`HlsMediaSource` bila `isStreamHls === true` **atau** URL mengandung `.m3u8`
/ berawal `hls://`; selain itu memakai `ProgressiveMediaSource`. Jadi Anda bahkan
bisa begitu:

```javascript
RoCatUI.addVideo("https://anichin.stream/hls/abc123.m3u8", "EP 1", true, true);
```

#### `RoCatUI.thumbnailPreview(url)` dan `RoCatUI.videoPreview(url)`
Peninggalan **backward-compatibility** — tetap tersedia dan berfungsi:

- `thumbnailPreview(url)` → setara `addImage(url)` (gambar saja, tanpa title).
  Kompatibel dengan skrip lama.
- `videoPreview(url)` → mode lama yang membuka video lewat `Intent.ACTION_VIEW`
  (keluar ke pemutar luar). **Disarankan beralih ke `addVideo`** yang memutar inline.

### 2.3 Layout — `RoCatUI.addGrid(columns, itemsJson, onClickFunction)`

Membuat grid media ala Mihon.

| Parameter | Tipe | Deskripsi |
|-----------|------|-----------|
| `columns` | `number` | Jumlah kolom (di-clamp `1..8`). |
| `itemsJson` | `string` | **JSON array** objek (lihat format di bawah). |
| `onClickFunction` | `string` | Nama fungsi yang dipanggil saat tile diketuk, dengan **payload JSON string** item tsb. |

**Format JSON**: array dari objek; tiap objek minimal membawa `title` dan `image`
(URL). Sifat tambahan lain (`id`, `url`, `episode`, dsb.) **dipertahankan** lalu
dikirim ulang ke skrip ketika tile diketuk.

```javascript
var results = [
    { id: "1", title: "Perfect World", image: "https://…/cover1.jpg", url: "https://…/seri/perf" },
    { id: "2", title: "Lingwu Continent", image: "https://…/cover2.jpg", url: "https://…/ser/ling" },
];
RoCatUI.addGrid(3, JSON.stringify(results), "openDetail");
```

Pola pemanggilannya di skrip:

```javascript
function openDetail(itemJsonString) {
    var item = JSON.parse(itemJsonString);   // kembali ke objek asli
    var u = item.url;                        // alamat detail yang mau dibuka
    // ...
}
```

> Payload yang bukan JSON array valid → grid tidak dirender (silakan cek output console).
> Grid item yang kosong `title`/`image` tetap dirender dengan placeholder.

### 2.4 Utilitas UI

#### `RoCatUI.clear()`
Menghapus semua komponen yang sedang dirender. Dipakai untuk "berpindah halaman" —
pola utama navigasi skrip: `clear()` lalu gambar ulang.

#### `RoCatUI.log(text)`
Menambahkan satu baris pesan ke area log skrip. Tempat utama untuk memberi umpan
balik kepada pengguna selama scraping.

```javascript
RoCatUI.log("⏳ Memuat " + n + " episode...");
if (!res.ok) RoCatUI.log("Gagal: status " + res.status);
```

---

## 3. DOM Parsing: global `RoCatDOM`

`RoCatDOM` adalah **bridge DOM native berbasis Jsoup**. Ia menggantikan Cheerio atau
Jsoup murni dari sisi skrip: skrip tidak perlu `import`/bundel apa pun.

### 3.1 Parsing

#### `RoCatDOM.parse(html)`
Mem-parsing string HTML dan mengembalikan element **root wrapper**. Biasakan:

```javascript
var root = RoCatDOM.parse(htmlSource);
```

#### Pintasan statis (tanpa root)

| Fungsi | Mengembalikan |
|--------|--------------|
| `RoCatDOM.select(html, selector)` | Array element wrapper dari semua match `selector`. |
| `RoCatDOM.selectText(html, selector)` | Teks match pertama (`""` bila tak ada). |
| `RoCatDOM.selectAttr(html, selector, attr)` | Nilai atribut match pertama (`""`). |
| `RoCatDOM.selectHtml(html, selector)` | Outer HTML match pertama (`""`). |
| `RoCatDOM.has(html, selector)` | `boolean` — apakah ada match. |

Semua pemilihan memakai **selector CSS** yang didukung Jsoup (mis. `.eplister ul li a`,
`select.mirror option`, `script[type="application/ld+json"]`).

### 3.2 Metode Element (wrapper)

Setiap element wrapper (root maupun hasil `.find()`/`select()`) punya anggota:

| Properti/Fungsi | Tipe hasil | Keterangan |
|----------------|-----------|------------|
| `.text` | `string` | Teks elemen (trimmed). |
| `.html` | `string` | Outer HTML (termasuk tag sendiri). |
| `.innerHtml` | `string` | Inner HTML (hanya children). |
| `.attrs` | `object` | Peta nama → nilai semua atribut. |
| `.attr(name)` | `string` | Nilai atribut `name`, `""` bila absen. |
| `.has(selector)` | `boolean` | Elemen sendiri match `selector`. |
| `.contains(selector)` | `boolean` | Ada descendant yang match `selector`. |
| `.find(selector)` | `array` | Semua descendant match → array element baru. |
| `.textOf(selector)` | `string` | Teks descendant pertama yang match. |
| `.attrOf(selector, attr)` | `string` | Atribut descendant pertama yang match. |
| `.textsOf(selector)` | `array<string>` | Teks semua descendant yang match. |
| `.nextElement(selector)` | element \| `null` | Sibling berikutnya yang match `selector`. |

### 3.3 Contoh Ekstraksi (Anichin/Manga)

**Daftar episode dari halaman detail:**

```javascript
var doc = RoCatDOM.parse(html);
var eps = doc.find(".eplister ul li a");
var epItems = [];
for (var i = 0; i < eps.length; i++) {
    var a = eps[i];
    var url = a.attr("href");
    if (!url) continue;
    epItems.push({
        title: a.textOf(".epl-num") || ("Episode " + (i + 1)),
        image: "",
        url: url
    });
}
RoCatUI.addGrid(3, JSON.stringify(epItems), "openEpisode");
```

**Judul + sinopsis:**

```javascript
var title   = root.textOf(".entry-title");       // teks pertama
var synopsis= root.textOf(".synp .entry-content");// ""
var cover   = root.attrOf(".thumb img", "src");
```

**Atribut + sibling berikutnya (contoh MangaUpdates):**

```javascript
var valueBox = keys[i].nextElement(".info-box ... sContent");
```

---

## 4. Network & Utilities

### 4.1 `fetch(url, options)` — sinkron

`fetch` adalah fungsi **sinkron** bawaan yang melewati OkHttp client aplikasi.

```javascript
var res = fetch("https://anichin.cafe/", "GET", {}, null);
var html = res.text();
```

Dua bentuk pemanggilan:

```javascript
// Bentuk posisional: fetch(url, method, headers, body)
fetch(url, "POST", { "Content-Type": "application/json" }, '{"q":"naruto"}');

// Bentuk options: fetch(url, { method, headers, body })
fetch(url, { method: "GET", headers: { "X-Custom": "1" } });
```

**Objek Response** yang dikembalikan (autentik, tanpa promise):

| Anggota | Tipe | Keterangan |
|---------|------|------------|
| `.status` | `number` | Kode HTTP. `0` bila gagal jaringan. |
| `.statusText` | `string` | Pesan status. |
| `.ok` | `boolean` | `status` 200–299. |
| `.body` | `string` | Raw body response. |
| `.headers` | `object` | Map `name → value` (nilai pertama). |
| `.error` | `string?` | Pesan error bila request gagal (`null`/undefined). |
| `.text()` | `string` | Body sebagai string. |
| `.json()` | any | Parsing body sebagai JSON; **throws** bila body kosong/invalid. |

**Never throws untuk error jaringan**: koneksi gagal / timeout / DNS dilaporkan lewat
`.error` + `.status === 0`, bukan exception — skrip tidak bisa “gantung” atau crash app.
Timeout per-call pada klien skrip: connect 10 s, read 10 s, call 30 s.

### 4.2 Stealth & Interceptor (otomatis)

Setiap request `fetch()` melewati stack HTTP aplikasi sehingga **otomatis** mendapat:

1. **Cloudflare Bypasser** — `CloudflareInterceptor` mendeteksi challenge
   (HTTP 403/503 + `Server: cloudflare` + “Just a moment…”) lalu menyelesaikannya via
   WebView headless, mengambil `cf_clearance` ke cookie jar bersama, dan **retry** request.
2. **User-Agent kustom** — `UserAgentInterceptor` menempel UA browser-grade
   (default `Chrome/141`) bila belum diset. UA bisa diatur pengguna di **Settings → Jaringan**.
3. **Header stealth browser** — `StealthHeadersInterceptor` menambah `Accept-Language`,
   `Sec-CH-UA*`, `Sec-Fetch-*` default Chromium bila skrip/request tidak menentukannya.
4. **Custom DNS / DoH** — `DoHResolver` (System / Cloudflare / Google / Quad9 / kustom)
   sesuai pengaturan; fallback ke DNS sistem bila DoH gagal.
5. **Cookie jar bersama** — OkHttp berbagi cookie identik dengan WebView in-app
   (`AndroidCookieJar`): sesi login lewat tab **Browser** otomatis terpakai scraper.

> **Urutan prioritas:** header yang ANDA berikan secara eksplisit selalu menang
> atas nilai default (UA/stealth). Bila `headers` berisi `User-Agent`, itu yang dipakai.

Skrip mendapat berita terbaru secara otomatis: engine **di-rebuild** oleh `ScriptManager`
bila user mengubah User-Agent/DNS, jadi Anda tidak perlu menulis ulang apa pun.

### 4.3 Utilitas Native

#### `RoCatUI.decodeBase64(str)`
Dekode Base64 → UTF-8 menggunakan **decoder native** (`android.util.Base64` di app,
fallback `java.util.Base64` di test). Jauh lebih cepat untuk blobs iframe yang besar
(jangan menyalin decoder JS di semua skrip).

| Perilaku | Hasil |
|----------|-------|
| Input valid | string UTF-8 ter-decode. |
| Padding salah / whitespace | dipad & dibersihkan otomatis. |
| Gagal / bukan Base64 | `""` (empty string, tidak pernah throw). |

```javascript
var raw = opt.attr("value");              // "PGlmcmFtZSBzcmM9Imh0dHBzOi8vLi4u" 
var decoded = RoCatUI.decodeBase64(raw);
var m = decoded.match(/src=\"(.*?)\"/);
if (m) console.log("iframe:", m[1]);
```

#### `RoCatUI.save(fileName, content, mimeType)`
Menulis `content` sebagai file **nyata** ke folder scrape milik skrip
`[MainDirectory]/Scrapes/<scriptId>/` (SAF/StorageManager). Return: **string URI** file
yang tertulis, atau `""` bila gagal.

```javascript
var uri = RoCatUI.save("result.json", JSON.stringify(data), "application/json");
if (uri !== "") RoCatUI.log("Tersimpan: " + uri);
```

Parameter `mimeType` default `text/plain`. Nama file dinormalisasi aplikasi.

---

## 5. Contoh Skrip Lengkap (Boilerplate)

Contoh fiktif yang menggabungkan semua API: metadata, `onLaunch`, formulir pencarian,
`fetch`, grid, detail, dan pemutaran video HLS.

```javascript
// ==UserScript==
// @name         Rakun Anime Scraper (Contoh)
// @version      1.0.0
// @description  Boilerplate: onLaunch -> pencarian -> grid detail -> streaming HLS.
// @author       RoCat AI
// @category     Anime
// @icon         https://example.com/icon.png
// @match        https://contoh.anime/*
// ==/UserScript==

var BASE = "https://contoh.anime";
var UA   = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0 Mobile Safari/537.36";

// --- Lifecycle: dipanggil otomatis saat kanvas dibuka ---
function onLaunch() {
    try {
        RoCatUI.clear();
        RoCatUI.addInput("keyword", "Cari anime / donghua...");
        RoCatUI.addButton("Search", "doSearch");
        RoCatUI.log("⏳ Memuat rilisan terbaru...");

        var res = fetch(BASE + "/", "GET", {}, null);
        if (res.ok) {
            var items = parseCards(res.text());
            if (items.length > 0) {
                RoCatUI.log("Ditemukan " + items.length + " judul. Ketuk untuk detail.");
                RoCatUI.addGrid(3, JSON.stringify(items), "openDetail");
            } else {
                RoCatUI.log("Tidak ada kartu di home — gunakan pencarian di atas.");
            }
        } else {
            RoCatUI.log("⚠ Gagal memuat home (" + res.status + ")— gunakan pencarian.");
        }
    } catch (e) {
        RoCatUI.log("❌ onLaunch: " + e.message);
    }
}

// ---▶ Dipanggil tombol "Search" — menerima objek { keyword: "..." } ---
function doSearch(inputs) {
    try {
        var q = (inputs && inputs.keyword || "").trim();
        if (q === "") { RoCatUI.log("⚠ Masukkan kata kunci."); return; }

        RoCatUI.clear();
        RoCatUI.addButton("🏠 Home", "onLaunch");
        RoCatUI.log("⏳ Mencari \"" + q + "\"...");

        var res = fetch(BASE + "/search?q=" + encodeURIComponent(q), "GET", {}, null);
        if (!res.ok) { RoCatUI.log("❌ Pencarian gagal (" + res.status + ")."); return; }

        var items = parseCards(res.text());
        RoCatUI.log("✓ " + items.length + " hasil.");
        RoCatUI.addGrid(2, JSON.stringify(items), "openDetail");
    } catch (e) {
        RoCatUI.log("❌ doSearch: " + e.message);
    }
}

// --- Dipanggil saat tile grid diketuk — payload JSON string ---
function openDetail(payload) {
    try {
        var item = JSON.parse(payload);
        RoCatUI.clear();
        RoCatUI.addButton("🏠 Home", "onLaunch");
        RoCatUI.log("⏳ Detail: " + item.title);

        var res = fetch(item.url, "GET", {}, null);
        if (!res.ok) { RoCatUI.log("❌ Detail gagal (" + res.status + ")."); return; }

        var doc = RoCatDOM.parse(res.text());
        RoCatUI.addImage(doc.attrOf(".cover img", "src") || item.image, item.title, true);
        RoCatUI.log(doc.textOf(".sinopsis") || "No synopsis.");

        var eps = doc.find(".episode-list a");
        var epList = [];
        for (var i = 0; i < eps.length; i++) {
            var u = eps[i].attr("href");
            if (u) { epList.push({ title: "Episode " + (i + 1), image: "", url: u }); }
        }
        if (epList.length === 0) { RoCatUI.log("⚠ Tidak ada episode."); return; }
        RoCatUI.addGrid(3, JSON.stringify(epList), "playEpisode");
    } catch (e) {
        RoCatUI.log("❌ openDetail: " + e.message);
    }
}

// ---▶ Dipanggil saat episode diketuk — panggil pemutar HLS native ---
function playEpisode(payload) {
    try {
        var ep = JSON.parse(payload);
        RoCatUI.clear();
        RoCatUI.addButton("🏠 Home", "onLaunch");
        RoCatUI.log("⏳ Menyiapkan stream: " + ep.title);

        // Contoh: URL master playlist .m3u8 dari sebuah server streaming.
        var hlsUrl = "https://cdn.contoh.anime/hls/ep" + (ep.url || "").replace(/\D+/g, "") + ".m3u8";

        // isStreamHls = true => ExoPlayer memakai HlsMediaSource.
        RoCatUI.addVideo(hlsUrl, ep.title, true, true);
        RoCatUI.log("🎬 Putar inline dengan ExoPlayer native (fullscreen tersedia).");
    } catch (e) {
        RoCatUI.log("❌ playEpisode: " + e.message);
    }
}

// --- Helper internal (RoCatDOM) ---
function parseCards(html) {
    var root = RoCatDOM.parse(html);
    var cards = root.find(".anime-card");
    var out = [];
    for (var i = 0; i < cards.length; i++) {
        var el = cards[i];
        var url = el.attrOf("a", "href");
        if (!url) continue;
        out.push({
            title: el.textOf("h2") || el.textOf("h3"),
            image: el.attrOf("img", "src") || "",
            url: url
        });
    }
    return out;
}
```

---

## 6. Praktik Terbaik & Batasan

1. **Selalu bungkus dalam `try/catch`** dan bicara via `RoCatUI.log`. Error tidak
   menggagalkan app, tapi pengguna akan tahu sebab.
2. **Satu kanvas dimulai dari `onLaunch()`** — jangan lakukan fetch berat di luar
   fungsi (misal langsung saat load). Interaksi apa pun diawali oleh JavaScript.
3. **Navigasi = `RoCatUI.clear()` + gambar ulang** (pola stack manual: simpan state
   di variabel global skrip bila perlu).
4. **Biasakan memilah varian HLS**: untuk stream seperti `anichin.stream`, fetch master
   `.m3u8`, seleksi varian ber-resolusi, dan kirim URL varian `isStreamHls=true` agar
   ExoPlayer tidak gagal mem-parse master yang ada catatan `#EXT-X-STREAM-INF` tanpa URI.
5. **Data besar** (Base64 blob, file scrape) → utamakan `RoCatUI.decodeBase64` dan
   `RoCatUI.save` (native, sinkron, dikendalikan).
6. **Jangan andalkan `console.log` tanpa Rhino console** di luar Canvas: gunakan
   `RoCatUI.log` untuk umpan balik visual.

---

## Lampiran: Referensi Sumber Implementasi

| Global | Sumber |
|--------|--------|
| `RoCatDOM` | `scripting/rhino/.../JsoupBridge.kt` |
| `RoCatUI` | `scripting/api/.../ScriptUiBridge.kt` + `scripting/rhino/.../RhinoScriptEngine.kt` |
| `fetch` | `scripting/api/.../network/ScriptFetch.kt` + `BridgeFetch` (Rhino) |
| Canvas / lifecycle | `app/.../ui/canvas/ScriptCanvasViewModel.kt` (entry `onLaunch`) |
| Pemutar Media3 / HLS | `app/.../ui/components/RocatVideoPlayer.kt` |
| Grid | `app/.../ui/components/GridView.kt` + `ScriptUIComponent.parseGrid` |
| Network stealth / DoH / UA | `core/.../network/NetworkHelper.kt`, interceptor CF & Stealth |
| Metadata Parser | `domain/.../script/ScriptMetadataParser.kt` |

> Skrip nyata yang memakai seluruh API ini: `scrape_anichin.js` di root repo —
> gunakan sebagai referensi kerja untuk pola `onLaunch` → `doSearch` → `openDetail`
> → `openEpisode` + penanganan HLS sungguhan (decode base64 → master `.m3u8` →
> pilih varian → `RoCatUI.addVideo(..., true, true)`).