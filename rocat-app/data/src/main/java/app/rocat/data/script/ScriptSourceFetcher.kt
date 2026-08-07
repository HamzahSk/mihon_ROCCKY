package app.rocat.data.script

import app.rocat.core.common.network.GET
import app.rocat.core.common.network.awaitSuccessString
import okhttp3.OkHttpClient

/**
 * Downloads a raw `.js` script from a URL. GitHub blob URLs are transparently
 * rewritten to their `raw.githubusercontent.com` equivalent.
 */
class ScriptSourceFetcher(
    private val client: OkHttpClient,
) {
    suspend fun fetchSource(url: String): String {
        val effectiveUrl = rewriteUrl(url)
        return client.newCall(GET(effectiveUrl, cacheControl = null)).awaitSuccessString()
    }

    private fun rewriteUrl(url: String): String {
        val githubBlob = Regex("^https?://github\\.com/([^/]+)/([^/]+)/blob/(.+)$")
        return githubBlob.replace(url) { match ->
            val (owner, repo, path) = match.destructured
            "https://raw.githubusercontent.com/$owner/$repo/$path"
        }
    }
}
