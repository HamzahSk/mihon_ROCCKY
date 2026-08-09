// ==UserScript==
// @name         Anichin Scraper
// @version      3.0.0
// @description  Cari, baca detail, dan streaming HLS (.m3u8) anime dari Anichin via RoCatUI.addVideo
// @author       RoCat AI
// @category     Anime
// @icon         https://anichin.cafe/wp-content/uploads/2023/06/cropped-Anichin-Logo-3-192x192.png
// ==/UserScript==

/**
 * Anichin Scraper v3 — skrip canvas RoCat (Tahap 22).
 *
 * Ditulis ulang memakai API universal `RoCat` (wrapper inti yang di-inject otomatis
 * oleh RhinoScriptEngine) + template UI baru:
 *   - `RoCat.render([...])`   → menggambar seluruh kanvas dengan satu panggilan.
 *   - `RoCat.safeParseJson()` → parsing payload yang tidak pernah throw.
 *   - `RoCatUI.addAlert(...)` → kartu status/info/warning/error.
 *   - `RoCatUI.addBadgeGroup(...)` → genre & status episode sebagai chip.
 *   - `RoCatUI.addJsonLog(...)`/`addHtmlPreview(...)`/`addAudio(...)` untuk tipe data lain.
 *
 * Alur: onLaunch (home + pencarian) -> doSearch (grid hasil) -> openDetail
 * (detail seri + daftar episode) -> openEpisode (ekstrak iframe base64,
 * parse master m3u8 anichin.stream, pilih varian terbaik, lalu panggil
 * RoCatUI.addVideo(..., isStreamHls = true) untuk pemutar Media3 native).
 *
 * Pembatasan Rhino: skrip TIDAK boleh memakai async/await, spread, optional
 * chaining, atau class. Semua jaringan memakai fetch() sinkron (OkHttp).
 */

var BASE_URL = "https://anichin.cafe";
var STREAM_HOST = "anichin.stream";
var B64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

/** Entry point otomatis — dipanggil canvas setiap kali layar kanvas dibuka. */
function onLaunch() {
    try {
        RoCat.render([
            { type: "clear" },
            { type: "input", id: "query", hint: "Cari Anime / Donghua (contoh: Perfect World)..." },
            { type: "button", label: "🔍 Cari", fn: "doSearch" }
        ]);
        RoCatUI.log("⏳ Memuat rilisan terbaru dari Anichin...");

        var res = fetch(BASE_URL + "/", "GET", {}, null);
        if (res.ok) {
            var items = parseAnimeCards(res.text());
            if (items.length > 0) {
                RoCatUI.log("✅ " + items.length + " rilisan tersedia. Ketuk salah satu untuk membuka episode.");
                RoCatUI.addGrid(3, JSON.stringify(items), "openDetail");
            } else {
                RoCatUI.addAlert("Homepage tidak mengembalikan kartu anime. Gunakan pencarian di atas.", "warning");
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
            { type: "input", id: "query", hint: "Cari Anime (contoh: Donghua)..." },
            { type: "button", label: "🔍 Cari Lagi", fn: "doSearch" }
        ]);
        RoCatUI.log("⏳ Mencari \"" + q + "\"...");

        var res = fetch(BASE_URL + "/page/1?s=" + encodeURIComponent(q), "GET", {}, null);
        if (!res.ok) {
            RoCatUI.addAlert("Gagal mengambil hasil (status " + res.status + ").", "error");
            return;
        }

        var items = parseAnimeCards(res.text());
        if (items.length === 0) {
            RoCatUI.addAlert("Tidak ada hasil untuk \"" + q + "\".", "info");
            return;
        }

        RoCatUI.log("✅ Ditemukan " + items.length + " hasil. Ketuk untuk melihat daftar episode.");
        RoCatUI.addGrid(3, JSON.stringify(items), "openDetail");
    } catch (e) {
        RoCatUI.log("❌ Error doSearch: " + e.message);
    }
}

/** Dipanggil saat item grid (hasil pencarian / home) diklik. */
function openDetail(payloadStr) {
    try {
        var item = RoCat.safeParseJson(payloadStr, {});
        RoCatUI.clear();
        RoCatUI.addButton("🏠 Menu Utama", "onLaunch");
        RoCatUI.log("⏳ Memuat detail \"" + (item.title || "") + "\"...");

        var res = fetch(item.url, "GET", {}, null);
        if (!res.ok) {
            RoCatUI.addAlert("Gagal memuat detail (status " + res.status + ").", "error");
            return;
        }

        var root = RoCatDOM.parse(res.text());
        var title = root.textOf(".entry-title") || item.title || "";
        var cover = root.attrOf(".thumb img", "src");
        var synopsis = root.textOf(".synp .entry-content");

        RoCatUI.clear();
        RoCatUI.addButton("🏠 Menu Utama", "onLaunch");
        if (cover !== "") RoCatUI.addImage(cover, title, true);
        RoCatUI.log(title + (synopsis !== "" ? "\n\nSinopsis: " + synopsis.trim() : ""));

        // Genre + status sebagai chip (Tahap 22.2) — hanya bila tersedia di halaman.
        var badges = parseDetailBadges(root);
        if (badges.length > 0) RoCatUI.addBadgeGroup(JSON.stringify(badges));

        var eps = root.find(".eplister ul li a");
        var epItems = [];
        for (var i = 0; i < eps.length; i++) {
            var el = eps[i];
            var epUrl = el.attr("href");
            if (!epUrl) continue;
            var epNum = el.textOf(".epl-num");
            epItems.push({
                title: (epNum ? epNum.trim() : ("Episode " + (i + 1))),
                image: "",
                url: epUrl
            });
        }

        if (epItems.length === 0) {
            RoCatUI.addAlert("Tidak ada daftar episode di halaman ini.", "info");
            return;
        }

        RoCatUI.log("🎬 " + epItems.length + " episode tersedia. Ketuk salah satu untuk memutar.");
        RoCatUI.addGrid(3, JSON.stringify(epItems), "openEpisode");
    } catch (e) {
        RoCatUI.log("❌ Error openDetail: " + e.message);
    }
}

/** Dipanggil saat item daftar episode diklik. */
function openEpisode(payloadStr) {
    try {
        var item = RoCat.safeParseJson(payloadStr, {});
        RoCatUI.clear();
        RoCatUI.addButton("🏠 Menu Utama", "onLaunch");
        RoCatUI.log("⏳ Menyiapkan stream untuk \"" + (item.title || "") + "\"...");

        var res = fetch(item.url, "GET", {}, null);
        if (!res.ok) {
            RoCatUI.addAlert("Gagal memuat halaman episode (status " + res.status + ").", "error");
            return;
        }

        var doc = RoCatDOM.parse(res.text());
        var options = doc.find("select.mirror option");
        var added = 0;
        var mirrors = [];

        for (var i = 0; i < options.length; i++) {
            var opt = options[i];
            if (!opt.attr("data-index")) continue; // opsi placeholder "Select Video Server"

            var b64 = opt.attr("value");
            if (!b64) continue;

            var decoded = "";
            try { decoded = decodeBase64(b64); } catch (e) { continue; }
            var m = decoded.match(/src="(.*?)"/);
            if (!m || !m[1]) continue;

            var iframeUrl = m[1];
            var serverName = opt.text ? opt.text.trim() : "";

            if (iframeUrl.indexOf(STREAM_HOST) !== -1) {
                var master = masterPlaylistUrl(iframeUrl);
                var best = pickBestVariant(master);
                var playUrl = best !== null ? best : master;
                RoCatUI.addVideo(playUrl, (item.title || "Anime") + " · " + (serverName || "Stream"), true, true);
                added++;
            } else {
                mirrors.push((serverName || "Mirror") + ": " + iframeUrl);
            }
        }

        if (added > 0) {
            RoCatUI.addAlert("Stream HLS siap! Tekan 'Play Inline' untuk memutar, atau ikon fullscreen untuk layar penuh.", "success");
        } else {
            RoCatUI.addAlert("Tidak ada stream HLS dari server utama. Mirror alternatif:", "warning");
            for (var j = 0; j < mirrors.length; j++) RoCatUI.log("  • " + mirrors[j]);
            RoCatUI.log("💡 Buka mirror di atas lewat tab Browser di aplikasi.");
        }
    } catch (e) {
        RoCatUI.log("❌ Error openEpisode: " + e.message);
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

/** Memparsing kartu anime (.bixbox article .bsx) menjadi item grid. */
function parseAnimeCards(html) {
    var root = RoCatDOM.parse(html);
    var cards = root.find(".bixbox article .bsx");
    var out = [];
    var seen = {};
    for (var i = 0; i < cards.length; i++) {
        var el = cards[i];
        var url = el.attrOf("a", "href");
        if (!url || seen[url]) continue;
        seen[url] = true;
        var title = el.textOf(".tt h2");
        if (!title) title = el.textOf(".tt");
        var cover = el.attrOf("img", "src");
        out.push({ title: title.trim(), image: cover, url: url });
    }
    return out;
}

/**
 * Menyusun chip badge (genre + status/tipe) dari halaman detail Anichin.
 * Best-effort: selector yang berubah-ubah tidak boleh mematahkan skrip.
 */
function parseDetailBadges(root) {
    var badges = [];
    try {
        var genres = root.find(".genxed a");
        for (var i = 0; i < genres.length; i++) {
            var g = genres[i].text.trim();
            if (g !== "") badges.push(g);
        }
        var infos = root.find(".tsinfo .imptdt");
        for (var j = 0; j < infos.length; j++) {
            var label = infos[j].textOf("i") || infos[j].textOf("span:first-child") || "";
            var value = infos[j].textOf("div:last-child") || infos[j].textOf(".data") || "";
            if (!label || !value) continue;
            if (label.indexOf("Status") !== -1 || label.indexOf("Tipe") !== -1 || label.indexOf("Type") !== -1) {
                var v = value.trim();
                if (v !== "") badges.push(label.trim() + ": " + v);
            }
        }
    } catch (e) { /* best-effort */ }
    return badges;
}

/** Membangun URL master playlist dari URL iframe anichin.stream. */
function masterPlaylistUrl(iframeUrl) {
    var m = iframeUrl.match(/^([a-z]+:\/\/[^\/]+)[\/?].*[?&]id=([^&]+)/);
    if (!m || !m[2]) return iframeUrl;
    return m[1] + "/hls/" + m[2] + ".m3u8";
}

/**
 * Mengambil master m3u8 dan memilih varian dengan resolusi tertinggi.
 * Master anichin.stream sering memiliki baris #EXT-X-STREAM-INF tanpa URI
 * (mis. varian 1080p), sehingga URL varian yang VALID dikirim ke pemutar
 * alih-alih master (ExoPlayer akan gagal parse master yang malformed).
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
                if (pending !== null) pending = null; // STREAM-INF tanpa URI -> buang
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

/** Resolve URL relatif terhadap URL master. */
function resolveUrl(base, url) {
    if (url.indexOf("http://") === 0 || url.indexOf("https://") === 0) return url;
    var m = base.match(/^([a-z]+:\/\/[^\/]+)\//);
    if (m) return m[1] + "/" + url.replace(/^\//, "");
    return url;
}

/** Decode base64: pakai bridge native RoCatUI.decodeBase64 (Tahap 20), fallback decoder murni JS bila bridge tak ada. */
function decodeBase64(input) {
    var s = String(input);
    try {
        if (typeof RoCatUI !== "undefined" && RoCatUI.decodeBase64) {
            return RoCatUI.decodeBase64(s);
        }
    } catch (e) { /* lanjut ke decoder murni JS */ }
    return b64Decode(s);
}

function b64Decode(input) {
    var str = String(input).replace(/[^A-Za-z0-9+/=]/g, "");
    if (str.length % 4 !== 0) str = str + "====".slice(0, 4 - (str.length % 4));
    var out = "";
    for (var i = 0; i < str.length; i += 4) {
        var e1 = b64Val(str.charAt(i));
        var e2 = b64Val(str.charAt(i + 1));
        var e3 = b64Val(str.charAt(i + 2));
        var e4 = b64Val(str.charAt(i + 3));
        var c1 = ((e1 << 2) | (e2 >> 4)) & 0xFF;
        var c2 = (((e2 & 15) << 4) | (e3 >> 2)) & 0xFF;
        var c3 = (((e3 & 3) << 6) | e4) & 0xFF;
        out += String.fromCharCode(c1);
        if (e3 !== 64) out += String.fromCharCode(c2);
        if (e4 !== 64) out += String.fromCharCode(c3);
    }
    return out;
}

function b64Val(ch) {
    if (ch === "" || ch === "=") return 64;
    return B64_CHARS.indexOf(ch);
}
