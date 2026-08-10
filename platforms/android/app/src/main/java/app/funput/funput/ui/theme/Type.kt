package app.funput.funput.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Vietnamese stacks two marks on one vowel (ế, ự, ỡ), which reaches higher than the Latin ascender
 * Compose measures line height against. Centring the text in its line box and keeping the trim off
 * gives those marks room instead of clipping them in tight rows.
 */
private val VietnameseLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun funputStyle(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Double = 0.0,
) = TextStyle(
    fontFamily = FunputFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    lineHeightStyle = VietnameseLineHeight,
)

/**
 * The full Material 3 type scale. All fifteen styles are defined on purpose: any style left out
 * falls back to the Material default, which mixes a second set of line heights and letter spacing
 * into screens that otherwise share this one.
 */
internal val Typography = Typography(
    displayLarge = funputStyle(size = 57, lineHeight = 64, weight = FontWeight.Bold, letterSpacing = -0.5),
    displayMedium = funputStyle(size = 45, lineHeight = 52, weight = FontWeight.Bold, letterSpacing = -0.5),
    displaySmall = funputStyle(size = 36, lineHeight = 44, weight = FontWeight.Bold),
    headlineLarge = funputStyle(size = 32, lineHeight = 40, weight = FontWeight.Bold, letterSpacing = -0.5),
    headlineMedium = funputStyle(size = 28, lineHeight = 36, weight = FontWeight.SemiBold),
    headlineSmall = funputStyle(size = 24, lineHeight = 32, weight = FontWeight.SemiBold),
    titleLarge = funputStyle(size = 22, lineHeight = 28, weight = FontWeight.SemiBold),
    titleMedium = funputStyle(size = 16, lineHeight = 24, weight = FontWeight.SemiBold),
    titleSmall = funputStyle(size = 14, lineHeight = 20, weight = FontWeight.SemiBold),
    bodyLarge = funputStyle(size = 16, lineHeight = 24),
    bodyMedium = funputStyle(size = 14, lineHeight = 20),
    bodySmall = funputStyle(size = 12, lineHeight = 18),
    labelLarge = funputStyle(size = 14, lineHeight = 20, weight = FontWeight.SemiBold),
    labelMedium = funputStyle(size = 12, lineHeight = 16, weight = FontWeight.Medium, letterSpacing = 0.5),
    labelSmall = funputStyle(size = 11, lineHeight = 16, weight = FontWeight.Medium, letterSpacing = 0.5),
)
