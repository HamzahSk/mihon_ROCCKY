package app.rocat.data.script

import app.rocat.core.common.network.GET
import app.rocat.core.common.network.awaitSuccess
import okhttp3.OkHttpClient

/**
 * Downloads a raw `.js` script from a URL. GitHub blob URLs are transparently
 * rewritten to their `raw.githubusercontent.com` equivalent. The response is validated
 * to be plain text/JS (rejecting HTML error pages and other non-script payloads)
 * before it is handed to the importer for storage.
 */
class ScriptSourceFetcher(
    private val client: OkHttpClient,
) {
    suspend fun fetchSource(url: String): String {
        val effectiveUrl = rewriteUrl(url)
        val response = client.newCall(GET(effectiveUrl, cacheControl = null)).awaitSuccess()
        response.use { res ->
            val body = res.body?.string() ?: ""
            validateContentType(res.header("Content-Type"), effectiveUrl)
            validateBody(body, effectiveUrl)
            return body
        }
    }

    private fun validateContentType(contentType: String?, url: String) {
        val mime = contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
        if (mime.startsWith("text/html") || mime.contains("xml")) {
            throw IllegalArgumentException(
                "URL returned \"$mime\" instead of a script (expected plain text or JavaScript): $url",
            )
        }
    }

    private fun validateBody(body: String, url: String) {
        val trimmed = body.trimStart()
        if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) ||
            trimmed.startsWith("<html", ignoreCase = true)
        ) {
            throw IllegalArgumentException(
                "URL returned an HTML page instead of a script. " +
                    "Use a raw/link-to-file URL (e.g. raw.githubusercontent.com): $url",
            )
        }
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("URL returned an empty response: $url")
        }
    }

    private fun rewriteUrl(url: String): String {
        val githubBlob = Regex("^https?://github\\.com/([^/]+)/([^/]+)/blob/(.+)$")
        return githubBlob.replace(url) { match ->
            val (owner, repo, path) = match.destructured
            "https://raw.githubusercontent.com/$owner/$repo/$path"
        }
    }
}
