package app.rocat.ui.playground

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
}