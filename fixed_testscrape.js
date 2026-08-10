// ==UserScript==
// @name         XVideos Scraper
// @version      3.1.0
// @description  Cari, baca detail, dan streaming video dari xvideos via RoCatUI.addVideo
//               dengan dual-mode scraping: Mode Statis (fetch + RoCatDOM) + Mode
//               Interaktif (RoCatPage / headless WebView) untuk halaman yang butuh JS.
// @author       RoCat AI
// @category     Anime
// @icon         https://www.xvideos.com/favicon.ico
// ==/UserScript==

/**
 * XVideos Scraper v3.1 (fixed_testscrape.js) — skrip canvas RoCat (Tahap 23).
 *
 * Demonstrasi arsitektur DUAL-MODE scraping engine:
 *
 *   Mode Statis (ringan, default):
 *     `fetch()` + `RoCatDOM` untuk pencarian/home/detail — cepat & hemat daya.
 *
 *   Mode Interaktif (headless WebView, fallback):
 *     `RoCatPage` saat HTML statis TIDAK memuat player (anti-bot / JS-generated) —
 *     buka halaman di WebView tersembunyi, tunggu selector, eksekusi JS untuk
 *     mengekstrak html5player, lalu `close()`.
 *
 * API Tahap 22/23 yang dipakai:
 *   - `RoCat.render([...])`          → menggambar seluruh kanvas dengan satu panggilan.
 *   - `RoCat.safeParseJson(str, {})` → parsing payload grid yang tidak pernah throw.
 *   - `RoCatUI.addAlert(...)`        → banner info/warning/error untuk status.
 *   - `RoCatUI.addBadgeGroup(...)`   → genre/status sebagai chip.
 *   - `RoCatUI.addJsonLog(...)`      → debug hasil ekstraksi stream.
 *   - `RoCatPage.open/type/click/waitForSelector/evaluate/getHtml/close` (Tahap 23).
 *
 * Alur: onLaunch (home + pencarian) -> doSearch (grid hasil) -> openDetail
 * (detail video) -> openVideo (ekstrak URL dari script html5player, pilih
 * kualitas terbaik, lalu panggil RoCatUI.addVideo untuk pemutar Media3).
 *
 * Pembatasan Rhino: skrip TIDAK boleh memakai async/await, spread, optional
 * chaining, atau class. Semua jaringan memakai fetch() sinkron (OkHttp).
 */

var BASE_URL = "https://www.xvideos.com";

/** Entry point otomatis — dipanggil canvas setiap kali layar kanvas dibuka. */
function onLaunch() {
    try {
        RoCat.render([
            { type: "clear" },
            { type: "input", id: "query", hint: "Cari Video (contoh: judul video)..." },
            { type: "button", label: "🔍 Cari", fn: "doSearch" }
        ]);
        RoCatUI.log("⏳ Memuat video terbaru dari xvideos...");

        var res = fetch(BASE_URL + "/", "GET", {}, null);
        if (res.ok) {
            var items = parseVideoCards(res.text());
            if (items.length > 0) {
                RoCatUI.log("✅ " + items.length + " video tersedia. Ketuk salah satu untuk membuka.");
                RoCatUI.addGrid(3, JSON.stringify(items), "openDetail");
            } else {
                RoCatUI.addAlert("Homepage tidak mengembalikan kartu video. Gunakan pencarian di atas.", "warning");
            }
        } else {
            RoCatUI.addAlert("Gagal memuat homepage (status " + res.status + "). Gunakan pencarian di atas.", "error");
        }
    } catch (e) {
        RoCatUI.log("❌ Error onLaunch: " + e.message);
    }
}

/** Dipanggil saat tombol Cari ditekan. */
function doSearch(inputs) {
    try {
        var q = readInput(inputs, "query").trim();
        if (q === "") {
            RoCatUI.addAlert("Masukkan kata kunci pencarian terlebih dahulu!", "warning");
            return;
        }

        RoCat.render([
            { type: "clear" },
            { type: "button", label: "🏠 Menu Utama", fn: "onLaunch" },
            { type: "input", id: "query", hint: "Cari Video..." },
            { type: "button", label: "🔍 Cari Lagi", fn: "doSearch" },
            { type: "alert", message: "Mencari \"" + q + "\"...", level: "info" }
        ]);

        var res = fetch(BASE_URL + "/?k=" + encodeURIComponent(q) + "&p=0", "GET", {}, null);
        if (!res.ok) {
            RoCatUI.addAlert("Gagal mengambil hasil (status " + res.status + ").", "error");
            return;
        }

        var items = parseVideoCards(res.text());
        if (items.length === 0) {
            RoCatUI.addAlert("Tidak ada hasil untuk \"" + q + "\".", "info");
            return;
        }

        RoCatUI.log("✅ Ditemukan " + items.length + " hasil. Ketuk untuk membuka.");
        RoCatUI.addGrid(3, JSON.stringify(items), "openDetail");
    } catch (e) {
        RoCatUI.log("❌ Error doSearch: " + e.message);
    }
}

/** Dipanggil saat item grid diklik. */
function openDetail(payloadStr) {
    try {
        var item = RoCat.safeParseJson(payloadStr, {});
        if (!item || !item.url) {
            RoCatUI.addAlert("Item grid tidak valid.", "error");
            return;
        }

        RoCatUI.clear();
        RoCatUI.addButton("🏠 Menu Utama", "onLaunch");
        RoCatUI.log("⏳ Memuat detail \"" + (item.title || "") + "\"...");

        var res = fetch(item.url, "GET", {}, null);
        if (!res.ok) {
            RoCatUI.addAlert("Gagal memuat detail (status " + res.status + ").", "error");
            return;
        }

        var root = RoCatDOM.parse(res.text());
        var title = root.textOf("h2.page-title") || item.title || "";
        var cover = root.attrOf("meta[property=og:image]", "content");
        var author = root.textOf("li.main-uploader span.name");

        RoCatUI.clear();
        RoCat.render([
            { type: "button", label: "🏠 Menu Utama", fn: "onLaunch" }
        ]);
        if (cover !== "") RoCat.render({ type: "image", url: cover, title: title, download: true });
        RoCatUI.log(title + (author !== "" ? "\n\n👤 Uploader: " + author : ""));

        // Genre sebagai chip (Tahap 22.2) — hanya bila tersedia di halaman.
        var genres = root.textsOf("div.video-metadata ul li a.is-keyword");
        if (genres.length > 0) RoCatUI.addBadgeGroup(JSON.stringify(genres));

        // Simpan data untuk openVideo
        var videoData = { url: item.url, title: title };
        RoCatUI.addButton("▶️ Putar Video", "openVideo", JSON.stringify(videoData));
    } catch (e) {
        RoCatUI.log("❌ Error openDetail: " + e.message);
    }
}

/** Dipanggil saat tombol Putar Video diklik. */
function openVideo(payloadStr) {
    try {
        var item = RoCat.safeParseJson(payloadStr, {});
        if (!item || !item.url) {
            RoCatUI.addAlert("Payload video tidak valid.", "error");
            return;
        }

        RoCatUI.clear();
        RoCatUI.addButton("🏠 Menu Utama", "onLaunch");
        RoCatUI.addButton("🔙 Kembali", "openDetail", JSON.stringify({ url: item.url, title: item.title }));
        RoCatUI.log("⏳ Menyiapkan stream untuk \"" + (item.title || "") + "\"...");

        var res = fetch(item.url, "GET", {}, null);
        if (!res.ok) {
            RoCatUI.addAlert("Gagal memuat halaman video (status " + res.status + ").", "error");
            return;
        }

        var doc = RoCatDOM.parse(res.text());
        var script = extractPlayerScript(doc);

        // --- Dual-mode stream extraction (Tahap 23) ---
        var low = "";
        var high = "";
        var hls = "";
        if (script !== "") {
            // Mode Statis: HTML statis memuat script html5player → ekstrak langsung.
            low = extractVideoUrl(script, "setVideoUrlLow");
            high = extractVideoUrl(script, "setVideoUrlHigh");
            hls = extractVideoUrl(script, "setVideoHLS");
        } else {
            // Mode Interaktif: player di-generate via JS (anti-bot/iframe) sehingga
            // fetch() + Jsoup tidak bisa melihatnya. Render halaman di headless WebView,
            // tunggu <script>, lalu ekstrak lewat evaluate().
            RoCatUI.addAlert("Script html5player tidak ada di HTML statis — merender halaman via WebView...", "warning");
            var live = extractViaHeadless(item.url);
            if (live && (live.low || live.high || live.hls)) {
                low = live.low;
                high = live.high;
                hls = live.hls;
                RoCatUI.addJsonLog(live, "Hasil Mode Interaktif (RoCatPage)", true);
            }
        }

        var videos = [];
        if (low !== "" && low !== "undefined") videos.push({ url: low, quality: "Low" });
        if (high !== "" && high !== "undefined" && high !== low) videos.push({ url: high, quality: "High" });
        if (hls !== "" && hls !== "undefined") videos.push({ url: hls, quality: "HLS" });

        if (videos.length === 0) {
            RoCatUI.addAlert("Tidak ada video stream yang ditemukan.", "warning");
            RoCatUI.addJsonLog({ snippet: script.substring(0, 300) }, "Script html5player", true);
            return;
        }

        // Tampilkan semua kualitas yang tersedia (debug via JSON log card).
        var sources = [];
        for (var i = 0; i < videos.length; i++) sources.push({ quality: videos[i].quality, url: videos[i].url });
        RoCatUI.addJsonLog(sources, "Kualitas tersedia", true);

        // Pilih video dengan kualitas terbaik (prioritas: HLS > High > Low).
        var selected = videos[0];
        for (var j = 1; j < videos.length; j++) {
            if (videos[j].quality === "HLS") {
                selected = videos[j];
                break;
            } else if (videos[j].quality === "High" && selected.quality !== "HLS") {
                selected = videos[j];
            }
        }

        // Jika HLS, coba ekstrak varian terbaik dari master playlist.
        var playUrl = selected.url;
        var qualityLabel = selected.quality;
        if (selected.quality === "HLS" && playUrl.indexOf(".m3u8") !== -1) {
            var bestVariant = pickBestVariant(selected.url);
            if (bestVariant !== null) {
                playUrl = bestVariant;
                qualityLabel = "HLS (Best)";
            }
        }

        var isHls = (selected.quality === "HLS" || playUrl.indexOf(".m3u8") !== -1);
        RoCatUI.addVideo(playUrl, (item.title || "Video") + " · " + qualityLabel, isHls, true);
        RoCatUI.addAlert("Stream siap! Tekan 'Play Inline' untuk memutar, atau ikon fullscreen.", "success");
    } catch (e) {
        RoCatUI.log("❌ Error openVideo: " + e.message);
    }
}

/**
 * Mencari script html5player secara fault-tolerant. Penting: konten <script>
 * adalah CDATA — `text()` Jsoup mengembalikan "" (mis. `textOf` di draft lama
 * selalu gagal), jadi kita baca `innerHtml` yang memuat kode JS mentah.
 * Memakai `:containsData` (pseudo-selector Jsoup gaya script/style) lalu
 * fallback manual ke semua tag <script>.
 */
function extractPlayerScript(doc) {
    var selectors = [
        "script:containsData(html5player.setVideoUrlLow)",
        "script:containsData(setVideoHLS)",
        "script:containsData(html5player)"
    ];
    for (var s = 0; s < selectors.length; s++) {
        var matched = doc.find(selectors[s]);
        if (matched.length > 0) return matched[0].innerHtml;
    }

    var scripts = doc.find("script");
    for (var i = 0; i < scripts.length; i++) {
        if (scripts[i].innerHtml.indexOf("html5player") !== -1) {
            return scripts[i].innerHtml;
        }
    }
    return "";
}

/**
 * Mode Interaktif (Tahap 23): buka halaman video di headless WebView (`RoCatPage`),
 * tunggu <script> muncul, lalu eksekusi JS untuk mengekstrak URL player html5player
 * dari DOM yang sudah di-render — mengalahkan anti-bot / konten JS-generated yang
 * tidak terlihat oleh fetch() + Jsoup. WebView selalu ditutup di `finally`.
 */
function extractViaHeadless(url) {
    if (typeof RoCatPage === "undefined") return null;
    try {
        if (!RoCatPage.open(url, 20000)) {
            RoCatUI.addAlert("Mode Interaktif: gagal membuka halaman (timeout).", "warning");
            return null;
        }
        RoCatPage.waitForSelector("script", 8000);

        var js = "(function(){ " +
            "var out = { low: \"\", high: \"\", hls: \"\" }; " +
            "var scripts = document.querySelectorAll(\"script\"); " +
            "for (var i = 0; i < scripts.length; i++) { " +
            "  var t = scripts[i].textContent || \"\"; " +
            "  var m; " +
            "  if (!out.low && (m = t.match(/setVideoUrlLow\\(['\"]([^'\"]*)['\"]\\)/))) out.low = m[1]; " +
            "  if (!out.high && (m = t.match(/setVideoUrlHigh\\(['\"]([^'\"]*)['\"]\\)/))) out.high = m[1]; " +
            "  if (!out.hls && (m = t.match(/setVideoHLS\\(['\"]([^'\"]*)['\"]\\)/))) out.hls = m[1]; " +
            "} " +
            "return out; " +
            "})()";

        var data = RoCatPage.evaluate(js);
        if (!data || typeof data !== "object") {
            RoCatUI.addAlert("Mode Interaktif: JS tidak mengembalikan data player.", "warning");
            return null;
        }
        RoCatUI.addAlert("Mode Interaktif: halaman dirender WebView & player berhasil diekstrak.", "info");
        return { low: data.low || "", high: data.high || "", hls: data.hls || "" };
    } catch (e) {
        RoCatUI.addAlert("Mode Interaktif gagal: " + e.message, "error");
        return null;
    } finally {
        RoCatPage.close();
    }
}

/** Ekstrak URL sumber dari pemanggilan html5player, mis. setVideoHLS("..."). */
function extractVideoUrl(script, functionName) {
    try {
        var match = script.match(new RegExp(functionName + "\\('([^']*)'\\)"));
        if (match && match[1]) return match[1];
        match = script.match(new RegExp(functionName + '\\("([^"]*)"\\)'));
        if (match && match[1]) return match[1];
        return "";
    } catch (e) {
        return "";
    }
}

/** Membaca input RoCatUI (objek { id -> value }) dengan aman. */
function readInput(inputs, key) {
    if (!inputs) return "";
    if (typeof inputs.get === "function") {
        var v = inputs.get(key);
        return v === null || v === undefined ? "" : String(v);
    }
    var raw = inputs[key];
    return raw === null || raw === undefined ? "" : String(raw);
}

/** Memparsing kartu video (.thumb-block) menjadi item grid. */
function parseVideoCards(html) {
    try {
        var root = RoCatDOM.parse(html);
        var cards = root.find("div.thumb-block");
        var out = [];
        var seen = {};
        for (var i = 0; i < cards.length; i++) {
            var el = cards[i];
            var url = el.attrOf("div.thumb-inside div.thumb a", "href");
            if (!url || seen[url]) continue;
            seen[url] = true;

            // Bangun URL absolut.
            if (url.indexOf("http") !== 0) url = BASE_URL + url;

            // Ambil title & bersihkan dari teks dalam kurung siku.
            var titleEl = el.find("div.thumb-under p.title a");
            var title = "";
            if (titleEl.length > 0) title = String(titleEl[0].text || "").replace(/\[.*?\]/g, " ").replace(/\s+/g, " ").trim();
            if (title === "") title = "Unknown";

            var cover = el.attrOf("div.thumb-inside div.thumb img", "data-src");
            if (cover === "") cover = el.attrOf("div.thumb-inside div.thumb img", "src");

            out.push({ title: title, image: cover, url: url });
        }
        return out;
    } catch (e) {
        RoCatUI.log("⚠️ Error parseVideoCards: " + e.message);
        return [];
    }
}

/**
 * Mengambil master m3u8 dan memilih varian dengan resolusi tertinggi.
 * Master yang malformed (#EXT-X-STREAM-INF tanpa URI) dilewati sehingga URL
 * varian yang VALID dikirim ke pemutar alih-alih master.
 */
function pickBestVariant(masterUrl) {
    try {
        var res = fetch(masterUrl, "GET", {}, null);
        if (!res.ok) return null;

        var lines = String(res.text()).split("\n");
        var variants = [];
        var pending = null;
        for (var i = 0; i < lines.length; i++) {
            var line = lines[i].trim();
            if (line === "") {
                if (pending !== null) pending = null;
                continue;
            }
            if (line.indexOf("#EXT-X-STREAM-INF:") === 0) {
                pending = line.substring(17);
            } else if (line.charAt(0) !== "#" && pending !== null) {
                variants.push({ attrs: pending, url: resolveUrl(masterUrl, line) });
                pending = null;
            }
        }
        if (variants.length === 0) return null;

        var best = variants[0].url, bestScore = -1;
        for (var j = 0; j < variants.length; j++) {
            var score = variantScore(variants[j].attrs);
            if (score > bestScore) { bestScore = score; best = variants[j].url; }
        }
        return best;
    } catch (e) {
        return null;
    }
}

/** Skor varian: utamakan tinggi resolusi, fallback bandwidth. */
function variantScore(attrs) {
    var res = attrs.match(/RESOLUTION=(\d+)x(\d+)/);
    if (res) return parseInt(res[2], 10);
    var bw = attrs.match(/BANDWIDTH=(\d+)/);
    if (bw) return parseInt(bw[1], 10);
    return 0;
}

/** Resolve URL varian relatif terhadap URL master. */
function resolveUrl(base, url) {
    if (url.indexOf("http://") === 0 || url.indexOf("https://") === 0) return url;
    var m = base.match(/^([a-z]+:\/\/[^\/]+)\//);
    if (m) return m[1] + "/" + url.replace(/^\//, "");
    return url;
}