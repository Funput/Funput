package app.funput.funput.ui.kit.theme

import androidx.compose.ui.unit.Dp
import app.funput.funput.ui.kit.tokens.LayoutTokens
import app.funput.funput.ui.kit.tokens.RadiusTokens
import app.funput.funput.ui.kit.tokens.SpacingTokens
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import com.kyant.shapes.RoundedRectangularShape

/**
 * FunputUI shapes. Corners use continuous curvature (the curve flows into the straight edge
 * instead of meeting it at a visible seam), which is what makes a card read as one soft object.
 *
 * Every shape is a [RoundedRectangularShape], the only kind the glass effect can refract, so any
 * FunputUI surface can become glass without a crash.
 */
object FunputShapes {
    /** Cards and sheets. */
    val card: RoundedRectangularShape = RoundedRectangle(RadiusTokens.card)

    /** Tappable cards that stand out from plain ones. */
    val interactiveCard: RoundedRectangularShape = RoundedRectangle(RadiusTokens.interactiveCard)

    /** Theme and keyboard thumbnails. */
    val thumbnail: RoundedRectangularShape = RoundedRectangle(RadiusTokens.thumbnail)

    /** The tinted square behind a row icon. */
    val iconTile: RoundedRectangularShape = RoundedRectangle(RadiusTokens.iconTile)

    /** Small chips and badges. */
    val smallTile: RoundedRectangularShape = RoundedRectangle(RadiusTokens.smallTile)

    /** Fully rounded ends: buttons, the tab bar, segmented controls. */
    val capsule: RoundedRectangularShape = Capsule()
}

/** The only distances FunputUI puts between things. */
object FunputSpacing {
    /** Between a label and what it labels. */
    val tight: Dp = SpacingTokens.tight

    /** Between rows of one thing. */
    val small: Dp = SpacingTokens.small

    /** Inside a control: an icon and its text. */
    val medium: Dp = SpacingTokens.medium

    /** Inside a card, and between cards. */
    val large: Dp = SpacingTokens.large

    /** Between one section and the next. */
    val section: Dp = SpacingTokens.section

    /** The screen's side margin. */
    val pageMargin: Dp = LayoutTokens.pageMargin

    /** Padding inside a card. */
    val cardPadding: Dp = LayoutTokens.cardPadding

    /** Content never grows wider than this, so tablets and unfolded phones keep a readable column. */
    val contentMaxWidth: Dp = LayoutTokens.contentMaxWidth

    /** Width of a card's hairline. */
    val cardStrokeWidth: Dp = LayoutTokens.cardStrokeWidth
}
