package app.rocat.ui.playground

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Helpers used by the playground result/log card to format output before it is
 * displayed and copied to the clipboard.
 */
object ResultFormatter {

    /**
     * Returns [raw] pretty-printed as JSON when it is valid JSON; otherwise returns
     * the original text unchanged so the "Copy JSON" action degrades gracefully.
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun prettyJson(raw: String): String {
        val jsonElement = try {
            Json.parseToJsonElement(raw)
        } catch (e: Exception) {
            return raw
        }
        val format = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        return format.encodeToString(JsonElement.serializer(), jsonElement)
    }
}