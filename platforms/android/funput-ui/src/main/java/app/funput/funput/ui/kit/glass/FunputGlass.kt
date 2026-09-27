package app.funput.funput.ui.kit.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.kit.theme.FunputColors
import app.funput.funput.ui.kit.theme.FunputSpacing
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangularShape

/** The film over real glass, so labels keep contrast over busy content. */
private const val GlassTintAlpha = 0.28f

/** The film over frosted glass: thick enough that text behind reads as colour, not words. */
private const val FrostedTintAlpha = 0.55f

/**
 * Draws a FunputUI glass surface in [shape].
 *
 * The only place the glass library is called: screens and components never touch it, so swapping
 * the implementation is a change to this file. [shape] is a [RoundedRectangularShape] because the
 * library's refraction throws on any other shape; the type makes that crash impossible. [refract]
 * bends content at the edges; bars that span the full width turn it off. [frosted] blurs harder
 * under a thicker film, for small controls whose own labels must not compete with text passing
 * behind them (the tab bar). The [GlassTier.SOLID] surface is an opaque card with a hairline.
 */
internal fun Modifier.funputGlass(
    backdrop: GlassBackdrop?,
    shape: RoundedRectangularShape,
    colors: FunputColors,
    refract: Boolean = true,
    frosted: Boolean = false,
): Modifier {
    if (backdrop == null || backdrop.tier == GlassTier.SOLID) {
        return clip(shape)
            // Fully opaque: any see-through share turns the text scrolling behind into grey smears.
            .background(colors.cardBackground)
            .border(FunputSpacing.cardStrokeWidth, colors.cardStroke, shape)
    }
    return drawBackdrop(
        backdrop = backdrop.layer,
        shape = { shape },
        effects = {
            vibrancy()
            blur((if (frosted) FrostedBlur else ClearBlur).toPx())
            if (refract) lens(refractionHeight = 14.dp.toPx(), refractionAmount = 28.dp.toPx())
        },
        onDrawSurface = { drawRect(colors.cardBackground.copy(alpha = if (frosted) FrostedTintAlpha else GlassTintAlpha)) },
    )
}

private val ClearBlur = 3.dp
private val FrostedBlur = 14.dp
