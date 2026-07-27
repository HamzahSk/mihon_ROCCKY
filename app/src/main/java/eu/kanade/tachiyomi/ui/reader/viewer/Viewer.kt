package eu.kanade.tachiyomi.ui.reader.viewer

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters

/**
 * Interface for implementing a viewer.
 */
interface Viewer {

    /**
     * Called when the user stops scrolling and the viewer is idle.
     * Provides the page index and the visible view for OCR capture.
     */
    var onScrollStopped: ((pageIndex: Int, view: View) -> Unit)?
        get() = null
        set(_) {}

    /**
     * Returns the view this viewer uses.
     */
    fun getView(): View

    /**
     * Destroys this viewer. Called when leaving the reader or swapping viewers.
     */
    fun destroy() {}

    /**
     * Tells this viewer to set the given [chapters] as active.
     */
    fun setChapters(chapters: ViewerChapters)

    /**
     * Tells this viewer to move to the given [page].
     */
    fun moveToPage(page: ReaderPage)

    /**
     * Called from the containing activity when a key [event] is received. It should return true
     * if the event was handled, false otherwise.
     */
    fun handleKeyEvent(event: KeyEvent): Boolean

    /**
     * Called from the containing activity when a generic motion [event] is received. It should
     * return true if the event was handled, false otherwise.
     */
    fun handleGenericMotionEvent(event: MotionEvent): Boolean
}
