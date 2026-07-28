package tachiyomi.presentation.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import tachiyomi.presentation.core.util.FontManager
import tachiyomi.presentation.core.util.FontMetricsNormalizer

val Typography.header: TextStyle
    @Composable
    get() = bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )

/**
 * Creates a [Typography] that uses the custom font if available, with
 * smart metric adjustments applied to keep the layout consistent.
 *
 * When no custom font is set, the default Material 3 typography is returned.
 */
@Composable
fun rememberAppTypography(): Typography {
    val context = LocalContext.current
    val customFontFamily = FontManager.getFontFamily(context)
    val defaultTypography = Typography()

    return remember(customFontFamily) {
        if (customFontFamily == null) {
            defaultTypography
        } else {
            applyCustomFont(defaultTypography, customFontFamily, context)
        }
    }
}

/**
 * Applies the custom [fontFamily] to the [typography], adjusting each text
 * style's size, line height, and letter spacing based on font metrics.
 */
private fun applyCustomFont(
    typography: Typography,
    fontFamily: FontFamily,
    context: android.content.Context,
): Typography {
    val typeface = FontManager.getTypeface(context)
    val adjustment = if (typeface != null) {
        FontMetricsNormalizer.analyze(typeface)
    } else {
        FontMetricsNormalizer.FontAdjustment.NONE
    }

    val baseStyle = TextStyle(fontFamily = fontFamily)
    val adjustedBase = FontMetricsNormalizer.applyAdjustment(baseStyle, adjustment)

    // Build a helper that applies the custom font and adjustment to a style.
    fun applyTo(style: TextStyle): TextStyle {
        return style.copy(
            fontFamily = fontFamily,
            fontSize = if (adjustedBase.fontSize.isSpecified) adjustedBase.fontSize else style.fontSize,
            lineHeight = if (adjustedBase.lineHeight.isSpecified) adjustedBase.lineHeight else style.lineHeight,
            letterSpacing = if (adjustedBase.letterSpacing.isSpecified) adjustedBase.letterSpacing else style.letterSpacing,
        )
    }

    return Typography(
        displayLarge = applyTo(typography.displayLarge),
        displayMedium = applyTo(typography.displayMedium),
        displaySmall = applyTo(typography.displaySmall),
        headlineLarge = applyTo(typography.headlineLarge),
        headlineMedium = applyTo(typography.headlineMedium),
        headlineSmall = applyTo(typography.headlineSmall),
        titleLarge = applyTo(typography.titleLarge),
        titleMedium = applyTo(typography.titleMedium),
        titleSmall = applyTo(typography.titleSmall),
        bodyLarge = applyTo(typography.bodyLarge),
        bodyMedium = applyTo(typography.bodyMedium),
        bodySmall = applyTo(typography.bodySmall),
        labelLarge = applyTo(typography.labelLarge),
        labelMedium = applyTo(typography.labelMedium),
        labelSmall = applyTo(typography.labelSmall),
    )
}
