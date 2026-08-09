package app.rocat.scripting.api

/**
 * Callbacks invoked by a user script through the global `RoCatUI` object to build a
 * dynamic, script-driven Compose UI (the mihon-style extension tab). Implementations
 * typically queue new components into the ViewModel that owns the rendered list.
 *
 * MUST be safe to call from any thread: the Rhino engine evaluates scripts on a
 * background coroutine, so implementations are responsible for hopping back to the
 * main thread before touching Compose state.
 */
interface ScriptUiBridge {

    /** Renders an editable text field identified by [id] with the given [hint]. */
    fun addInput(id: String, hint: String)

    /**
     * Renders a button labelled [label] whose click re-invokes the script function
     * named [functionName], passing every input collected so far as a single object.
     */
    fun addButton(label: String, functionName: String)

    /** Renders the image at [url] (loaded with Coil). */
    fun thumbnailPreview(url: String)

    /** Renders a card/button that opens the video at [url] via `Intent.ACTION_VIEW`. */
    fun videoPreview(url: String)

    /**
     * Renders an image preview card (Tahap 18.1). [title] is shown above the image and
     * [allowDownload] toggles the "save to scrape folder" button on the card.
     */
    fun addImage(url: String, title: String = "", allowDownload: Boolean = true)

    /**
     * Renders a video preview card (Tahap 18.2/18.3) with an inline Media3 (ExoPlayer)
     * player and a download button. Set [isStreamHls] to `true` for `.m3u8` streams so
     * the player configures an HLS media source.
     */
    fun addVideo(
        url: String,
        title: String = "",
        isStreamHls: Boolean = false,
        allowDownload: Boolean = true,
    )

    /** Clears every currently rendered component. */
    fun clear()

    /**
     * Renders a responsive grid of [columns] columns. [itemsJsonString] is a JSON array
     * of objects (each expected to carry at least a `title` and `image`). Tapping a tile
     * re-invokes the script function named [onClickFunction], passing the tapped item's
     * JSON payload as a string argument — the script can then "navigate" by calling
     * [clear] and redrawing the detail UI.
     */
    fun addGrid(columns: Int, itemsJsonString: String, onClickFunction: String)

    /** Appends [text] to the script log area. */
    fun log(text: String)

    /**
     * Persists a scrape result. Implementations write [content] as a new file named
     * [fileName] inside the current scrape folder (e.g. via the StorageManager). This is
     * the Tahap 16.1 pipeline: scripts call `RoCatUI.save()` and the app genuinely writes
     * the bytes to device storage through the SAF content resolver.
     *
     * @return the content [android.net.Uri] string of the written file, or an empty
     *   string when the write failed.
     */
    fun saveFile(fileName: String, content: String, mimeType: String = "text/plain"): String
}