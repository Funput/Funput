package app.funput.funput.ui.kit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import app.funput.funput.ui.kit.R
import app.funput.funput.ui.kit.tokens.TypeToken
import app.funput.funput.ui.kit.tokens.TypeTokens

/**
 * Be Vietnam Pro, bundled so every device renders the same glyphs: it is drawn for Vietnamese,
 * so stacked marks (ế, ự, ỡ) are designed rather than assembled from a letter and a loose accent.
 */
val BeVietnamPro: FontFamily = FontFamily(
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
)

/**
 * Stacked Vietnamese marks rise above the Latin ascender that line height is measured against.
 * Centring text in its line box without trimming gives them room instead of clipping them.
 */
private val VietnameseLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/** The FunputUI type scale; every text in the app uses one of these six styles. */
@Immutable
class FunputTypography internal constructor(
    /** Large screen titles. */
    val display: TextStyle,
    /** Section-level and sheet titles. */
    val title: TextStyle,
    /** Emphasised row titles and card headings. */
    val headline: TextStyle,
    /** Default reading text. */
    val body: TextStyle,
    /** Buttons, tabs and group headers. */
    val label: TextStyle,
    /** Footnotes and helper text. */
    val caption: TextStyle,
) {
    internal companion object {
        /** The scale as defined by the design tokens, set in [BeVietnamPro]. */
        val Default: FunputTypography = FunputTypography(
            display = TypeTokens.display.style(),
            title = TypeTokens.title.style(),
            headline = TypeTokens.headline.style(),
            body = TypeTokens.body.style(),
            label = TypeTokens.label.style(),
            caption = TypeTokens.caption.style(),
        )

        private fun TypeToken.style() = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = weight,
            fontSize = size,
            lineHeight = lineHeight,
            lineHeightStyle = VietnameseLineHeight,
        )
    }
}
