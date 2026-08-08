package app.rocat.ui.import

import androidx.lifecycle.viewModelScope
import app.rocat.core.common.injekt.Injekt
import app.rocat.core.viewmodel.StateViewModel
import app.rocat.data.script.ScriptSourceFetcher
import app.rocat.domain.script.ImportScript
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class ImportScriptViewModel(
    private val importScript: ImportScript = Injekt.get(),
    private val scriptSourceFetcher: ScriptSourceFetcher = Injekt.get(),
) : StateViewModel<ImportScriptViewModel.State>(State()) {

    data class State(
        val url: String = "",
        val source: String = "",
        val busy: Boolean = false,
        val message: String? = null,
        val error: String? = null,
    )

    fun onUrlChange(value: String) = mutableState.update { it.copy(url = value) }
    fun onSourceChange(value: String) = mutableState.update { it.copy(source = value) }

    fun importFromUrl(onImported: (String) -> Unit) {
        val url = state.value.url.trim()
        if (url.isEmpty()) {
            mutableState.update { it.copy(error = "Enter a script URL first") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, error = null, message = null) }
            try {
                // URL normalization (scheme injection + GitHub blob rewrite) happens
                // inside the fetcher; network work runs on Dispatchers.IO there.
                val fetched = scriptSourceFetcher.fetchSource(url)
                val script = importScript.await(fetched)
                mutableState.update {
                    it.copy(busy = false, url = "", message = "Imported \"${script.name}\" v${script.version}")
                }
                onImported(script.id)
            } catch (e: Exception) {
                mutableState.update { it.copy(busy = false, error = friendlyMessage(e)) }
            }
        }
    }

    fun importFromSource(onImported: (String) -> Unit) {
        val source = state.value.source
        if (source.isBlank()) {
            mutableState.update { it.copy(error = "Paste script source first") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, error = null, message = null) }
            try {
                val script = importScript.await(source)
                mutableState.update {
                    it.copy(busy = false, source = "", message = "Imported \"${script.name}\" v${script.version}")
                }
                onImported(script.id)
            } catch (e: Exception) {
                mutableState.update { it.copy(busy = false, error = friendlyMessage(e)) }
            }
        }
    }

    fun loadExample() = mutableState.update { it.copy(source = EXAMPLE_SCRIPT) }

    companion object {
        /** Maps raw exceptions to messages a user can actually act on. */
        fun friendlyMessage(e: Throwable): String = when (e) {
            is IllegalArgumentException -> e.message ?: "Invalid input"
            is UnknownHostException -> "Cannot resolve the host (DNS error). Check the URL and your connection."
            is SocketTimeoutException -> "The connection timed out. Try again or use a different host."
            is SSLException -> "SSL/TLS handshake failed (untrusted certificate). ${e.message.orEmpty()}"
            is IOException -> "Network error: ${e.message ?: e.javaClass.simpleName}"
            else -> "Import failed: ${e.message ?: e.javaClass.simpleName}"
        }

        /**
         * A Rhino-compatible sample (no async/await, no imports) that uses the
         * built-in [app.rocat.scripting.rhino.RhinoScriptEngine] `fetch` bridge and
         * the `RoCatDOM` DOM bridge (Jsoup) instead of Cheerio. Exposes both
         * `search(query)` and `detail(url)` for the playground's Test Execution.
         */
        val EXAMPLE_SCRIPT = """
            // ==UserScript==
            // @name        MangaUpdates Searcher
            // @version     1.0.0
            // @description Search & detail for MangaUpdates (sync fetch + RoCatDOM).
            // @author      RoCat
            // @match       https://www.mangaupdates.com/*
            // @grant       none
            // ==/UserScript==

            function main(query) {
                return search(query);
            }

            function search(query) {
                var url = "https://www.mangaupdates.com/series?search=" +
                    encodeURIComponent(query) + "&perpage=10";
                var res = fetch(url, "GET", {}, null);
                if (!res.ok) {
                    return { error: "HTTP " + res.status, message: res.body };
                }
                return parseSearch(res.text());
            }

            function parseSearch(html) {
                var cards = RoCatDOM.parse(html).find(".col-12.col-lg-6.p-3.text");
                var results = [];
                for (var i = 0; i < cards.length; i++) {
                    var card = cards[i];
                    var url = card.attrOf('a[title="Click for Series Info"]', "href") || "";
                    var imgEls = card.find("img");
                    var parts = url.split("/");
                    results.push({
                        id: parts.length > 1 ? parts[parts.length - 2] : null,
                        slug: parts.length > 0 ? parts[parts.length - 1] : null,
                        title: card.textOf(".linked-name-module__9zptFq__name_underline"),
                        url: url,
                        image: imgEls.length > 0 ? imgEls[0].attr("src") : null,
                        adult: card.contains(".series-box-module__K7yETa__adult"),
                        genres: splitComma(card.textOf(".textsmall .text-truncate")),
                        description: collapse(card.textOf(".mu-markdown-module___SC9hG__mu_markdown")),
                        year: firstYear(lastText(card, "> .row .series-box-module__K7yETa__mw_flex .text")),
                        rating: card.textOf("b") || null
                    });
                }
                return results;
            }

            function detail(url) {
                var res = fetch(url, "GET", {}, null);
                if (!res.ok) {
                    return { error: "HTTP " + res.status, message: res.body };
                }
                return parseDetail(res.text());
            }

            function parseDetail(html) {
                var root = RoCatDOM.parse(html);
                var data = {};

                var json = {};
                var ld = root.find('script[type="application/ld+json"]');
                if (ld.length > 0 && ld[0].innerHtml) {
                    try { json = JSON.parse(ld[0].innerHtml); } catch (e) { json = {}; }
                }

                var alt = orNull(json.alternateName);
                var genres = orNull(json.genre);
                data.id = orNull(json.identifier);
                data.title = orNull(json.name);
                data.alternativeTitles = alt !== null ? alt : [];
                data.cover = orNull(json.image);
                data.url = orNull(json.url);
                data.synopsis = orNull(json.description);
                data.year = orNull(json.datePublished);
                data.genres = genres !== null ? genres : [];
                data.authors = mapNamedLinks(json.author, "name");
                data.publishers = mapNamedLinks(json.publisher, "name");

                var keys = root.find(".info-box-module__gIhiNW__sCat");
                for (var i = 0; i < keys.length; i++) {
                    var key = keys[i].text;
                    var valueBox = keys[i].nextElement(".info-box-module__gIhiNW__sContent");
                    if (valueBox === null) continue;

                    if (key === "Type") {
                        data.type = valueBox.text;
                    } else if (key === "Status in Country of Origin") {
                        data.status = collapse(valueBox.text);
                    } else if (key === "Licensed (in English)") {
                        data.licensed = valueBox.text;
                    } else if (key === "Completely Scanlated?") {
                        data.scanlated = valueBox.text;
                    } else if (key === "Anime Start/End Chapter") {
                        data.anime = valueBox.text;
                    } else if (key === "Associated Names") {
                        data.associatedNames = valueBox.textsOf("div");
                    } else if (key === "Groups Scanlating") {
                        data.groups = mapNamedLinks(valueBox.find("a"), "name");
                    } else if (key === "Related Series") {
                        data.relatedSeries = mapNamedLinks(valueBox.find("a"), "title");
                    } else if (key === "Recommendations") {
                        data.recommendations = mapNamedLinks(valueBox.find("a"), "title");
                    } else if (key === "Latest Release(s)") {
                        var relEls = valueBox.find("> div");
                        data.latestReleases = [];
                        for (var j = 0; j < relEls.length; j++) {
                            data.latestReleases.push(collapse(relEls[j].text));
                        }
                    } else {
                        data[slugify(key)] = collapse(valueBox.text);
                    }
                }
                return data;
            }

            function mapNamedLinks(list, keyName) {
                var arr = [];
                if (list === null || list === undefined) return arr;
                for (var i = 0; i < list.length; i++) {
                    var o = {};
                    o[keyName] = list[i].name;
                    o.url = orNull(list[i].url);
                    arr.push(o);
                }
                return arr;
            }

            function orNull(v) {
                return (v === undefined || v === null) ? null : v;
            }

            function firstYear(text) {
                var m = text.match(/\d{4}/);
                return m ? m[0] : null;
            }

            function lastText(el, selector) {
                var els = el.find(selector);
                return els.length > 0 ? collapse(els[els.length - 1].text) : "";
            }

            function splitComma(text) {
                var parts = text.split(",");
                var out = [];
                for (var i = 0; i < parts.length; i++) {
                    var v = parts[i].trim();
                    if (v.length > 0) out.push(v);
                }
                return out;
            }

            function collapse(text) {
                return text.replace(/\s+/g, " ").trim();
            }

            function slugify(text) {
                return text.toLowerCase().replace(/[^a-z0-9]+/g, "_").replace(/^_|_$/g, "");
            }
        """.trimIndent()
    }
}
