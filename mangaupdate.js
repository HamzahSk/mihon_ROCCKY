import * as cheerio from "cheerio";

async function search(query) {
    const res = await fetch(
        `https://www.mangaupdates.com/series?search=${encodeURIComponent(query)}&perpage=10`
    );

    const html = await res.text();

    return parseSearch(html);
}

function parseSearch(html) {
    const $ = cheerio.load(html);
    const results = [];

    $(".col-12.col-lg-6.p-3.text").each((_, el) => {
        const card = $(el);

        const title = card
            .find(".linked-name-module__9zptFq__name_underline")
            .first()
            .text()
            .trim();

        const url = card
            .find('a[title="Click for Series Info"]')
            .attr("href") || "";

        const image = card.find("img").attr("src") || null;

        const adult =
            card.find(".series-box-module__K7yETa__adult").length > 0;

        const genres = card
            .find(".textsmall .text-truncate")
            .text()
            .trim()
            .split(",")
            .map(v => v.trim())
            .filter(Boolean);

        const description = card
            .find(".mu-markdown-module___SC9hG__mu_markdown")
            .text()
            .replace(/\s+/g, " ")
            .trim();

        const infoText = card
            .find("> .row .series-box-module__K7yETa__mw_flex .text")
            .last()
            .text()
            .replace(/\s+/g, " ")
            .trim();

        const year = infoText.match(/\d{4}/)?.[0] ?? null;
        const rating = card.find("b").first().text().trim() || null;

        const slug = url.split("/").pop() || null;
        const id = url.split("/").at(-2) || null;

        results.push({
            id,
            slug,
            title,
            url,
            image,
            adult,
            genres,
            description,
            year,
            rating,
        });
    });

    return results;
}

export async function detail(url) {
    const res = await fetch(url);
    const html = await res.text();

    return parseDetail(html);
}

function parseDetail(html) {
    const $ = cheerio.load(html);

    // JSON-LD
    const json = JSON.parse(
        $('script[type="application/ld+json"]').first().html() || "{}"
    );

    const data = {
        id: json.identifier ?? null,
        title: json.name ?? null,
        alternativeTitles: json.alternateName ?? [],
        cover: json.image ?? null,
        url: json.url ?? null,
        synopsis: json.description ?? null,
        year: json.datePublished ?? null,
        genres: json.genre ?? [],
        authors: (json.author || []).map(v => ({
            name: v.name,
            url: v.url
        })),
        publishers: (json.publisher || []).map(v => ({
            name: v.name,
            url: v.url
        }))
    };

    // Semua info-box
    $(".info-box-module__gIhiNW__sCat").each((_, el) => {
        const key = $(el).text().trim();
        const valueBox = $(el).next(".info-box-module__gIhiNW__sContent");

        switch (key) {
            case "Type":
                data.type = valueBox.text().trim();
                break;

            case "Status in Country of Origin":
                data.status = valueBox.text().replace(/\s+/g, " ").trim();
                break;

            case "Licensed (in English)":
                data.licensed = valueBox.text().trim();
                break;

            case "Completely Scanlated?":
                data.scanlated = valueBox.text().trim();
                break;

            case "Anime Start/End Chapter":
                data.anime = valueBox.text().trim();
                break;

            case "Associated Names":
                data.associatedNames = valueBox
                    .find("div")
                    .map((_, x) => $(x).text().trim())
                    .get();
                break;

            case "Groups Scanlating":
                data.groups = valueBox
                    .find("a")
                    .map((_, x) => ({
                        name: $(x).text().trim(),
                        url: $(x).attr("href")
                    }))
                    .get();
                break;

            case "Related Series":
                data.relatedSeries = valueBox
                    .find("a")
                    .map((_, x) => ({
                        title: $(x).text().trim(),
                        url: $(x).attr("href")
                    }))
                    .get();
                break;

            case "Recommendations":
                data.recommendations = valueBox
                    .find("a")
                    .map((_, x) => ({
                        title: $(x).text().trim(),
                        url: $(x).attr("href")
                    }))
                    .get();
                break;

            case "Latest Release(s)":
                data.latestReleases = valueBox
                    .find("> div")
                    .map((_, x) => $(x).text().replace(/\s+/g, " ").trim())
                    .get();
                break;

            default:
                data[
                    key
                        .toLowerCase()
                        .replace(/[^a-z0-9]+/g, "_")
                        .replace(/^_|_$/g, "")
                ] = valueBox.text().replace(/\s+/g, " ").trim();
        }
    });

    return data;
}

// Contoh penggunaan
//const result = await search("turning");
//console.log(result);

const result = await detail(
    "https://www.mangaupdates.com/series/rccbc2h/turning"
);
console.log(result);