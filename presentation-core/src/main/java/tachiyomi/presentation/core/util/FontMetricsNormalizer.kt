package tachiyomi.presentation.core.util

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Analyzes font metrics and computes scaling factors to normalize
 * custom fonts so they integrate cleanly with the app's default typography.
 *
 * The problem this solves: third-party fonts often have unusual metrics
 * (e.g. larger x-height, tighter or looser letter spacing, unusual
 * ascender/descender ratios) that can break line height, baseline
 * alignment, and overall layout.
 *
 * Approach:
 *  - Sample the font at a reference size and measure its metrics.
 *  - Compare against the reference (Material default) metrics.
 *  - Derive a [FontAdjustment] containing a size multiplier, line-height
 *    multiplier, and letter-spacing offset that bring the custom font
 *    visually in line with the system font.
 */
object FontMetricsNormalizer {

    /**
     * Computed adjustments for a custom font.
     *
     * @param sizeMultiplier Multiply the original text size by this factor.
     * @param lineHeightMultiplier Multiply the original line-height by this factor.
     * @param letterSpacingSp Additional letter spacing in sp (can be negative).
     */
    data class FontAdjustment(
        val sizeMultiplier: Float = 1f,
        val lineHeightMultiplier: Float = 1f,
        val letterSpacingSp: Float = 0f,
    ) {
        companion object {
            val NONE = FontAdjustment()
        }
    }

    // Reference metrics measured from the default system font at 14sp.
    // These serve as the "target" we want the custom font to match.
    private const val REFERENCE_SIZE_SP = 14f
    private const val REFERENCE_ASCENT_RATIO = -0.21f
    private const val REFERENCE_DESCENT_RATIO = 0.05f
    private const val REFERENCE_LINE_HEIGHT_RATIO = 1.2f

    /**
     * Analyzes the given [typeface] and returns a [FontAdjustment] that
     * normalizes its metrics to match the reference.
     */
    fun analyze(typeface: Typeface): FontAdjustment {
        val paint = Paint().apply {
            this.typeface = typeface
            textSize = REFERENCE_SIZE_SP * 3f // Measure at larger size for precision
        }

        val metrics = paint.fontMetrics

        // Calculate the ratio of ascent and descent relative to text size.
        val ascentRatio = metrics.ascent / paint.textSize
        val descentRatio = metrics.descent / paint.textSize

        // Size multiplier: if the custom font's visual height is larger
        // than the reference, scale down; if smaller, scale up.
        val referenceVisualHeight = REFERENCE_ASCENT_RATIO + REFERENCE_DESCENT_RATIO
        val customVisualHeight = ascentRatio + descentRatio
        val sizeMultiplier = if (customVisualHeight != 0f) {
            // Clamp to a reasonable range to avoid extreme scaling.
            (referenceVisualHeight / customVisualHeight).coerceIn(0.75f, 1.3f)
        } else {
            1f
        }

        // Line-height multiplier: adjust based on the font's internal
        // leading and line height characteristics.
        val lineHeight = metrics.descent - metrics.ascent
        val lineHeightRatio = if (paint.textSize != 0f) {
            lineHeight / paint.textSize
        } else {
            REFERENCE_LINE_HEIGHT_RATIO
        }
        val lineHeightMultiplier = if (lineHeightRatio != 0f) {
            (REFERENCE_LINE_HEIGHT_RATIO / lineHeightRatio).coerceIn(0.8f, 1.4f)
        } else {
            1f
        }

        // Letter spacing: measure a sample string and compare width
        // against the reference. If the font is naturally wider, apply
        // negative letter spacing; if narrower, apply positive.
        val sampleText = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        val sampleWidth = paint.measureText(sampleText)
        // Reference width approximation for a standard font at this size.
        val referenceWidth = REFERENCE_SIZE_SP * 3f * 0.52f * sampleText.length
        val letterSpacingSp = if (referenceWidth > 0f) {
            val ratio = (sampleWidth / referenceWidth) - 1f
            // Convert to sp and clamp to a subtle range.
            (ratio * REFERENCE_SIZE_SP / sampleText.length).coerceIn(-0.05f, 0.05f)
        } else {
            0f
        }

        return FontAdjustment(
            sizeMultiplier = sizeMultiplier,
            lineHeightMultiplier = lineHeightMultiplier,
            letterSpacingSp = letterSpacingSp,
        )
    }

    /**
     * Applies the [adjustment] to a [TextStyle], returning a new style with
     * normalized size, line height, and letter spacing.
     */
    fun applyAdjustment(style: TextStyle, adjustment: FontAdjustment): TextStyle {
        if (adjustment == FontAdjustment.NONE) return style

        val newSize = if (style.fontSize.isSpecified) {
            (style.fontSize.value * adjustment.sizeMultiplier).sp
        } else {
            style.fontSize
        }

        val newLineHeight = if (style.lineHeight.isSpecified) {
            (style.lineHeight.value * adjustment.lineHeightMultiplier).sp
        } else {
            // Default line height for the adjusted size.
            (newSize.value * REFERENCE_LINE_HEIGHT_RATIO).sp
        }

        val newLetterSpacing = if (style.letterSpacing.isSpecified) {
            (style.letterSpacing.value + adjustment.letterSpacingSp).sp
        } else {
            adjustment.letterSpacingSp.sp
        }

        return style.copy(
            fontSize = newSize,
            lineHeight = newLineHeight,
            letterSpacing = newLetterSpacing,
        )
    }
}
