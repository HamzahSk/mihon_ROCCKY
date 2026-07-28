package tachiyomi.core.common.util.system

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import logcat.LogPriority

data class TextBounds(
    val leftFraction: Float,
    val topFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
)

data class CoverRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val isEmpty: Boolean get() = width <= 0 || height <= 0
}

data class CoverTemplate(
    val titleBounds: TextBounds,
    val langBounds: TextBounds? = null,
    val chapterBounds: TextBounds? = null,
)

data class TypeSetField(
    val text: String,
    val bounds: TextBounds,
    val color: Int = Color.WHITE,
    val shadowColor: Int = Color.BLACK,
    val shadowRadius: Float = 3f,
    val font: Typeface? = Typeface.DEFAULT_BOLD,
    val alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
)

object CoverTypeSetter {

    private const val MAX_FONT_SIZE = 200f
    private const val MIN_FONT_SIZE = 6f
    private const val BINARY_SEARCH_PRECISION = 0.5f

    fun applyToCover(
        cover: Bitmap,
        title: String,
        lang: String? = null,
        chapterCount: String? = null,
    ): Bitmap {
        return applyToCover(cover, defaultTemplate, title, lang, chapterCount)
    }

    fun applyToCover(
        cover: Bitmap,
        template: CoverTemplate,
        title: String,
        lang: String? = null,
        chapterCount: String? = null,
    ): Bitmap {
        val fields = mutableListOf<TypeSetField>()
        fields.add(
            TypeSetField(
                text = title,
                bounds = template.titleBounds,
                alignment = Layout.Alignment.ALIGN_NORMAL,
            ),
        )
        if (lang != null && template.langBounds != null) {
            fields.add(
                TypeSetField(
                    text = lang,
                    bounds = template.langBounds,
                    color = Color.parseColor("#E0E0E0"),
                    shadowRadius = 2f,
                ),
            )
        }
        if (chapterCount != null && template.chapterBounds != null) {
            fields.add(
                TypeSetField(
                    text = chapterCount,
                    bounds = template.chapterBounds,
                    color = Color.parseColor("#E0E0E0"),
                    shadowRadius = 2f,
                    font = Typeface.DEFAULT,
                ),
            )
        }
        return applyToCover(cover, fields)
    }

    fun applyToCover(
        cover: Bitmap,
        fields: List<TypeSetField>,
    ): Bitmap {
        val result = cover.copy(Bitmap.Config.ARGB_8888, true)
            ?: run {
                logcat(LogPriority.WARN) { "CoverTypeSetter: failed to copy bitmap, returning original" }
                return cover
            }
        val canvas = Canvas(result)

        for (field in fields) {
            if (field.text.isBlank()) continue
            val rect = field.bounds.toRect(cover.width, cover.height)
            if (rect.isEmpty) continue
            drawTextInRect(canvas, rect, field)
        }

        return result
    }

    private fun drawTextInRect(canvas: Canvas, rect: CoverRect, field: TypeSetField) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = field.color
            typeface = field.font
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
            isDither = true
            isSubpixelText = true
        }

        val optimalSize = fitTextToBounds(field.text, paint, rect)
        if (optimalSize <= 0f) return

        paint.textSize = optimalSize
        val layout = createWrappedLayout(field.text, paint, rect.width, field.alignment)

        val textHeight = layout.height.coerceAtMost(rect.height)
        val yOffset = rect.top + ((rect.height - textHeight) / 2f)

        val shadowPaint = TextPaint(paint).apply {
            color = field.shadowColor
            style = Paint.Style.STROKE
            strokeWidth = field.shadowRadius * 0.8f
            strokeJoin = Paint.Join.ROUND
        }
        val shadowLayout = createWrappedLayout(field.text, shadowPaint, rect.width, field.alignment)

        canvas.save()
        canvas.translate(rect.left.toFloat(), yOffset)
        shadowLayout.draw(canvas)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun fitTextToBounds(
        text: String,
        paint: TextPaint,
        rect: CoverRect,
    ): Float {
        if (text.isEmpty()) return 0f

        var low = MIN_FONT_SIZE
        var high = MAX_FONT_SIZE

        while (high - low > BINARY_SEARCH_PRECISION) {
            val mid = (low + high) / 2f
            paint.textSize = mid

            val layout = createWrappedLayout(text, paint, rect.width, Layout.Alignment.ALIGN_NORMAL)
            if (layout.height <= rect.height) {
                low = mid
            } else {
                high = mid
            }
        }

        paint.textSize = high
        val finalLayout = createWrappedLayout(text, paint, rect.width, Layout.Alignment.ALIGN_NORMAL)
        return if (finalLayout.height <= rect.height) high else low
    }

    internal fun createWrappedLayout(
        text: String,
        paint: TextPaint,
        maxWidth: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    ): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, maxWidth)
            .setAlignment(alignment)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
            .build()
    }
}

private const val DEFAULT_TITLE_WIDTH_FRACTION = 0.50f
private const val DEFAULT_TITLE_HEIGHT_FRACTION = 0.18f
private const val DEFAULT_TITLE_LEFT_FRACTION = 0.04f
private const val DEFAULT_TITLE_TOP_FRACTION = 0.04f

private const val DEFAULT_LANG_WIDTH_FRACTION = 0.25f
private const val DEFAULT_LANG_HEIGHT_FRACTION = 0.05f
private const val DEFAULT_LANG_LEFT_FRACTION = 0.04f
private const val DEFAULT_LANG_TOP_FRACTION = 0.04f + 0.18f + 0.02f

private const val DEFAULT_CHAPTER_WIDTH_FRACTION = 0.35f
private const val DEFAULT_CHAPTER_HEIGHT_FRACTION = 0.06f
private const val DEFAULT_CHAPTER_LEFT_FRACTION = 0.04f
private const val DEFAULT_CHAPTER_TOP_FRACTION = 1f - 0.06f - 0.04f

internal val defaultTemplate = CoverTemplate(
    titleBounds = TextBounds(
        leftFraction = DEFAULT_TITLE_LEFT_FRACTION,
        topFraction = DEFAULT_TITLE_TOP_FRACTION,
        widthFraction = DEFAULT_TITLE_WIDTH_FRACTION,
        heightFraction = DEFAULT_TITLE_HEIGHT_FRACTION,
    ),
    langBounds = TextBounds(
        leftFraction = DEFAULT_LANG_LEFT_FRACTION,
        topFraction = DEFAULT_LANG_TOP_FRACTION,
        widthFraction = DEFAULT_LANG_WIDTH_FRACTION,
        heightFraction = DEFAULT_LANG_HEIGHT_FRACTION,
    ),
    chapterBounds = TextBounds(
        leftFraction = DEFAULT_CHAPTER_LEFT_FRACTION,
        topFraction = DEFAULT_CHAPTER_TOP_FRACTION,
        widthFraction = DEFAULT_CHAPTER_WIDTH_FRACTION,
        heightFraction = DEFAULT_CHAPTER_HEIGHT_FRACTION,
    ),
)

internal fun TextBounds.toRect(width: Int, height: Int): CoverRect {
    return CoverRect(
        left = (leftFraction * width).toInt(),
        top = (topFraction * height).toInt(),
        right = ((leftFraction + widthFraction) * width).toInt(),
        bottom = ((topFraction + heightFraction) * height).toInt(),
    )
}
