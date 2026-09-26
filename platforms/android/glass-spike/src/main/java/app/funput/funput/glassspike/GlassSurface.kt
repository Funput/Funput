package app.funput.funput.glassspike

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val Hairline = Color.Black.copy(alpha = 0.08f)

/** Everything a glass surface may sample, created once per screen. */
@Stable
class GlassSources(val mode: GlassMode, val backdrop: LayerBackdrop, val haze: HazeState) {
    /** Marks the content that glass surfaces sample. Only the active technique records it. */
    fun Modifier.glassSource(): Modifier = when (mode) {
        GlassMode.SOLID -> this
        GlassMode.HAZE -> hazeSource(haze)
        GlassMode.LIQUID -> layerBackdrop(backdrop)
    }

    /**
     * Draws a glass surface in [shape] using the active technique. [refract] bends the content at
     * the edges (tier 3 only); the backdrop library throws unless [shape] is corner-based.
     */
    fun Modifier.glass(shape: CornerBasedShape, refract: Boolean = true): Modifier = when (mode) {
        GlassMode.SOLID -> clip(shape)
            .background(Color.White.copy(alpha = 0.86f))
            .border(0.5.dp, Hairline, shape)
        GlassMode.HAZE -> clip(shape)
            .hazeBlur(
                input = HazeInput.Sources(haze),
                style = HazeBlurStyle {
                    blurRadius(24.dp)
                    backgroundColor(Color(0xFFF2F2F7))
                },
            )
            .background(Color.White.copy(alpha = 0.35f))
            .border(0.5.dp, Hairline, shape)
        GlassMode.LIQUID -> drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(3.dp.toPx())
                if (refract) lens(refractionHeight = 14.dp.toPx(), refractionAmount = 28.dp.toPx())
            },
            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.25f)) },
        )
    }
}

/** Remembers the sources for [mode]. */
@Composable
fun rememberGlassSources(mode: GlassMode): GlassSources {
    val backdrop = rememberLayerBackdrop()
    val haze = rememberHazeState()
    return remember(mode, backdrop, haze) { GlassSources(mode, backdrop, haze) }
}
