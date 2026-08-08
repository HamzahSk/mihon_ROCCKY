package app.rocat.ui.playground

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A single renderable unit produced by a script through the global `RoCatUI` object.
 * The playground renders each [ScriptUIComponent] in the order it was added while the
 * script drives the UI (mihon-style extension tab).
 */
sealed class ScriptUIComponent {

    /** An editable text field keyed by a script-chosen [id]; its current value is
     *  collected and forwarded back to the script when a button is pressed. */
    data class Input(
        val id: String,
        val hint: String,
        val value: String = "",
    ) : ScriptUIComponent()

    /** A button that re-invokes the script function named [functionName]. */
    data class Button(
        val label: String,
        val functionName: String,
    ) : ScriptUIComponent()

    /** An image preview rendered with Coil. */
    data class Thumbnail(
        val url: String,
    ) : ScriptUIComponent()

    /** A video card/button that opens [url] with the system video player. */
    data class Video(
        val url: String,
    ) : ScriptUIComponent()

    /** A single line appended to the script's log area. */
    data class LogText(
        val text: String,
    ) : ScriptUIComponent()

    /**
     * A responsive media grid (mihon-style search results). Tapping a tile re-invokes
     * the script's [onClickFunction] passing the item's raw JSON payload as a string.
     */
    data class Grid(
        val columns: Int,
        val items: List<GridItem>,
        val onClickFunction: String,
    ) : ScriptUIComponent()
}

/**
 * A single tile inside a [ScriptUIComponent.Grid]. [title] and [imageUrl] are the two
 * shared fields every grid item is expected to carry; [rawJsonPayload] keeps the full
 * original JSON object (including any extra custom fields) to hand back to the script.
 */
data class GridItem(
    val title: String,
    val imageUrl: String,
    val rawJsonPayload: String,
)

/**
 * Best-effort parser for the JSON array passed to `RoCatUI.addGrid(...)`. Produces a
 * [ScriptUIComponent.Grid] (or null when the payload is not a usable JSON array) whose
 * tiles keep their original JSON so clicking can forward the exact object back to JS.
 */
fun parseGrid(
    columns: Int,
    itemsJson: String,
    onClickFunction: String,
): ScriptUIComponent.Grid? {
    val elements = try {
        Json.parseToJsonElement(itemsJson) as? JsonArray ?: return null
    } catch (e: Exception) {
        return null
    }
    val items = elements.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        GridItem(
            title = (obj["title"] as? JsonPrimitive)?.content.orEmpty(),
            imageUrl = (obj["image"] as? JsonPrimitive)?.content?.trim().orEmpty(),
            rawJsonPayload = element.toString(),
        )
    }
    if (items.isEmpty()) return null
    return ScriptUIComponent.Grid(columns.coerceAtLeast(1), items, onClickFunction)
}