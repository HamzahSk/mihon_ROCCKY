package tachiyomi.core.common.util.system

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CoverTypeSetterTest {

    @Test
    fun `default template title bounds are valid fractions`() {
        val title = defaultTemplate.titleBounds
        assert(title.leftFraction >= 0f) { "leftFraction should be >= 0" }
        assert(title.topFraction >= 0f) { "topFraction should be >= 0" }
        assert(title.widthFraction > 0f) { "widthFraction should be > 0" }
        assert(title.heightFraction > 0f) { "heightFraction should be > 0" }
        assert(title.leftFraction + title.widthFraction <= 1f) {
            "title bounds should not exceed right edge"
        }
        assert(title.topFraction + title.heightFraction <= 1f) {
            "title bounds should not exceed bottom edge"
        }
    }

    @Test
    fun `default template lang bounds are valid fractions`() {
        val lang = defaultTemplate.langBounds
        assertNotNull(lang)
        lang?.let {
            assert(it.leftFraction >= 0f)
            assert(it.topFraction >= 0f)
            assert(it.widthFraction > 0f)
            assert(it.heightFraction > 0f)
            assert(it.leftFraction + it.widthFraction <= 1f) {
                "lang bounds should not exceed right edge"
            }
            assert(it.topFraction + it.heightFraction <= 1f) {
                "lang bounds should not exceed bottom edge"
            }
        }
    }

    @Test
    fun `default template chapter bounds are valid fractions`() {
        val chapter = defaultTemplate.chapterBounds
        assertNotNull(chapter)
        chapter?.let {
            assert(it.leftFraction >= 0f)
            assert(it.topFraction >= 0f)
            assert(it.widthFraction > 0f)
            assert(it.heightFraction > 0f)
            assert(it.leftFraction + it.widthFraction <= 1f) {
                "chapter bounds should not exceed right edge"
            }
            assert(it.topFraction + it.heightFraction <= 1f) {
                "chapter bounds should not exceed bottom edge"
            }
        }
    }

    @Test
    fun `text bounds to rect produces correct dimensions`() {
        val bounds = TextBounds(
            leftFraction = 0.1f,
            topFraction = 0.2f,
            widthFraction = 0.5f,
            heightFraction = 0.3f,
        )
        val rect = bounds.toRect(1000, 500)

        assertEquals(100, rect.left)
        assertEquals(100, rect.top)
        assertEquals(600, rect.right)
        assertEquals(250, rect.bottom)
        assertEquals(500, rect.width)
        assertEquals(150, rect.height)
    }

    @Test
    fun `text bounds to rect handles edge positions`() {
        val bounds = TextBounds(
            leftFraction = 0f,
            topFraction = 0f,
            widthFraction = 1f,
            heightFraction = 1f,
        )
        val rect = bounds.toRect(800, 600)

        assertEquals(0, rect.left)
        assertEquals(0, rect.top)
        assertEquals(800, rect.right)
        assertEquals(600, rect.bottom)
        assertEquals(800, rect.width)
        assertEquals(600, rect.height)
    }

    @Test
    fun `text bounds to rect handles small dimensions`() {
        val bounds = TextBounds(
            leftFraction = 0.25f,
            topFraction = 0.25f,
            widthFraction = 0.5f,
            heightFraction = 0.5f,
        )
        val rect = bounds.toRect(100, 100)

        assertEquals(25, rect.left)
        assertEquals(25, rect.top)
        assertEquals(75, rect.right)
        assertEquals(75, rect.bottom)
        assertEquals(50, rect.width)
        assertEquals(50, rect.height)
    }

    @Test
    fun `cover template default contains all three fields`() {
        val template = defaultTemplate
        assertEquals("TextBounds", template.titleBounds::class.simpleName)
        assertNotNull(template.langBounds)
        assertNotNull(template.chapterBounds)
    }

    @Test
    fun `text bounds should be ordered correctly`() {
        val title = defaultTemplate.titleBounds
        val lang = defaultTemplate.langBounds
        val chapter = defaultTemplate.chapterBounds

        // Title should be above lang
        assert(title.topFraction < lang!!.topFraction) {
            "title should be positioned above lang"
        }

        // Chapter should be near bottom
        assert(chapter!!.topFraction > title.topFraction) {
            "chapter should be positioned below title"
        }
    }

    private fun assertNotNull(value: Any?) {
        if (value == null) throw AssertionError("Expected non-null value")
    }
}
