// ==UserScript==
// @name         Anichin Scraper
// @version      1.1.5
// @description  Cari, baca detail, dan streaming HLS anime dari Anichin
// @author       RoCat AI
// @category     Anime
// @icon         https://anichin.cafe/wp-content/uploads/2023/06/cropped-Anichin-Logo-3-192x192.png
// ==/UserScript==

const BASE_URL = "https://anichin.cafe";

/**
 * Entry point otomatis saat skrip dijalankan di Canvas.
 */
function onLaunch() {
    RoCatUI.clear();
    
    // 1. Tampilkan UI Pencarian
    RoCatUI.addInput("query", "Cari Anime (contoh: Donghua)...");
    RoCatUI.addButton("🔍 Cari", "doSearch");
    
    RoCatUI.log("⏳ Menyiapkan video rekomendasi (Lingwu Continent Ep 179)...");
    
    // 2. Langsung load URL spesifik di Home
    const targetUrl = "https://anichin.cafe/lingwu-continent-episode-179-subtitle-indonesia/";
    try {
        const res = fetch(targetUrl, "GET", {}, null);
        
        if (res.ok) {
            const doc = RoCatDOM.parse(res.text());
            const options = doc.find("select.mirror option");
            let foundVideo = false;
            
            // [PERBAIKAN]: Gunakan .length properti dan indeks array [i]
            for (let i = 0; i < options.length; i++) {
                const opt = options[i];
                if (!opt.attr("data-index")) continue;
                
                const b64Val = opt.attr("value");
                if (!b64Val) continue;
                
                // Decode Base64 menggunakan utilitas bawaan Android di dalam Rhino
                const decodedStr = new java.lang.String(android.util.Base64.decode(b64Val, 0)).toString();
                const match = decodedStr.match(/src="(.*?)"/);
                
                if (match && match[1]) {
                    const iframeUrl = match[1];
                    const serverName = opt.text ? opt.text.trim() : "";
                    
                    // Filter khusus untuk anichin.stream
                    if (iframeUrl.indexOf("anichin.stream") !== -1) {
                        const m3u8Url = iframeUrl.replace("?id=", "hls/") + ".m3u8";
                        RoCatUI.addVideo(m3u8Url, "Lingwu Continent Ep 179 (" + serverName + ")", true, true);
                        foundVideo = true;
                        break; 
                    }
                }
            }
            
            if (foundVideo) {
                RoCatUI.log("✅ Video siap! Tap 'Play Inline' untuk menonton.");
            } else {
                RoCatUI.log("⚠️ Stream HLS untuk video ini tidak ditemukan di server utama.");
            }
        } else {
            RoCatUI.log("❌ Gagal memuat halaman video. Status: " + res.status);
        }
    } catch (e) {
        RoCatUI.log("❌ Terjadi kesalahan saat memuat video rekomendasi: " + e.message);
    }
}

/**
 * Dipanggil saat tombol Cari ditekan.
 */
function doSearch(inputs) {
    let q = "";
    if (inputs) {
        if (typeof inputs.get === "function") {
            q = inputs.get("query"); 
        } else {
            q = inputs.query; 
        }
    }

    if (!q || q.trim() === "") {
        RoCatUI.log("⚠️ Masukkan kata kunci pencarian terlebih dahulu!");
        return;
    }

    RoCatUI.clear();
    RoCatUI.addInput("query", "Cari Anime (contoh: Donghua)...");
    RoCatUI.addButton("🔍 Cari", "doSearch");
    RoCatUI.log("⏳ Sedang mencari: " + q + "...");

    const url = BASE_URL + "/page/1?s=" + encodeURIComponent(q);
    const res = fetch(url, "GET", {}, null);
    
    if (!res.ok) {
        RoCatUI.log("❌ Gagal mengambil data. Status: " + res.status);
        return;
    }

    const doc = RoCatDOM.parse(res.text());
    const items = doc.find(".bixbox article .bsx");
    
    const gridItems = [];
    // [PERBAIKAN]: Gunakan .length dan [i], serta textOf/attrOf bawaan RoCatDOM
    for (let i = 0; i < items.length; i++) {
        const el = items[i];
        const title = el.textOf(".tt h2");
        const itemUrl = el.attrOf("a", "href");
        const cover = el.attrOf("img", "src");
        
        gridItems.push({
            title: title ? title.trim() : "",
            imageUrl: cover || "",
            rawJsonPayload: JSON.stringify({ url: itemUrl, title: title })
        });
    }

    if (gridItems.length > 0) {
        RoCatUI.log("✅ Ditemukan " + gridItems.length + " hasil.");
        RoCatUI.addGrid(3, JSON.stringify(gridItems), "openDetail");
    } else {
        RoCatUI.log("⚠️ Tidak ada hasil yang ditemukan.");
    }
}

/**
 * Dipanggil saat item di Grid Pencarian diklik.
 */
function openDetail(payloadStr) {
    const payload = JSON.parse(payloadStr);
    RoCatUI.clear();
    RoCatUI.addButton("⬅️ Kembali ke Menu Utama", "onLaunch");
    RoCatUI.log("⏳ Memuat detail: " + payload.title + "...");

    const res = fetch(payload.url, "GET", {}, null);
    const doc = RoCatDOM.parse(res.text());
    
    // [PERBAIKAN]: Gunakan textOf dan attrOf dari ROOT document
    const title = doc.textOf(".entry-title");
    const cover = doc.attrOf(".thumb img", "src");
    const synopsis = doc.textOf(".synp .entry-content");

    RoCatUI.clear();
    RoCatUI.addButton("⬅️ Kembali ke Menu Utama", "onLaunch");
    
    RoCatUI.addImage(cover || "", title || "", false);
    RoCatUI.log("Sinopsis:\n" + (synopsis ? synopsis.trim() : "") + "\n\n🎬 Daftar Episode:");

    const eps = doc.find(".eplister ul li a");
    const epItems = [];
    
    for (let i = 0; i < eps.length; i++) {
        const el = eps[i];
        const epNum = el.textOf(".epl-num");
        const epUrl = el.attr("href");
        
        epItems.push({
            title: epNum ? epNum.trim() : "",
            imageUrl: "", 
            rawJsonPayload: JSON.stringify({ url: epUrl, title: payload.title + " - " + epNum })
        });
    }

    RoCatUI.addGrid(3, JSON.stringify(epItems), "openEpisode");
}

/**
 * Dipanggil saat item di Grid Episode diklik.
 */
function openEpisode(payloadStr) {
    const payload = JSON.parse(payloadStr);
    RoCatUI.clear();
    RoCatUI.addButton("🏠 Menu Utama", "onLaunch");
    RoCatUI.log("⏳ Menyiapkan video: " + payload.title + "...");

    const res = fetch(payload.url, "GET", {}, null);
    const doc = RoCatDOM.parse(res.text());
    const options = doc.find("select.mirror option");

    let foundVideo = false;

    for (let i = 0; i < options.length; i++) {
        const opt = options[i];
        if (!opt.attr("data-index")) continue;
        
        const b64Val = opt.attr("value");
        if (!b64Val) continue;

        const decodedStr = new java.lang.String(android.util.Base64.decode(b64Val, 0)).toString();
        
        const match = decodedStr.match(/src="(.*?)"/);
        if (match && match[1]) {
            const iframeUrl = match[1];
            const serverName = opt.text ? opt.text.trim() : "";

            if (iframeUrl.indexOf("anichin.stream") !== -1) {
                const m3u8Url = iframeUrl.replace("?id=", "hls/") + ".m3u8";
                RoCatUI.addVideo(m3u8Url, serverName + " (Stream Langsung)", true, true);
                foundVideo = true;
            } else {
                RoCatUI.log("🔗 Mirror Alternatif (" + serverName + ") : \n" + iframeUrl);
            }
        }
    }

    if (!foundVideo) {
        RoCatUI.log("\n⚠️ Video HLS native tidak ditemukan. Silakan gunakan link mirror alternatif di atas.");
    } else {
        RoCatUI.log("\n✅ Video siap! Tap 'Play Inline' atau gunakan ikon Fullscreen di ujung kanan atas pemutar.");
    }
}
