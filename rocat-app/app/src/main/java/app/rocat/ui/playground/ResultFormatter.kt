package app.rocat.ui.playground

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Helpers used by the playground result/log card to format output before it is
 * copied to the clipboard.
 */
object ResultFormatter {

    /**
     * Returns [raw] pretty-printed as JSON when it is valid JSON; otherwise returns
     * the original text unchanged so the "Copy JSON" action degrades gracefully.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun prettyJson(raw: String): String {
        val pretty = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            ignoreUnknownKeys = true
        }
        return try {
            val element: JsonElement = Json.parseToJsonElement(raw)
            pretty.encodeToString(JsonElement.serializer(), element)
        } catch (e: Exception) {
            raw
        }
    }
}