package app.funput.funput.ui.kit.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * What glass surfaces on one screen sample: the screen's content, recorded into a layer when the
 * tier is [GlassTier.GLASS]. A screen owns one and marks its scrolling content with [glassSource];
 * its bars then read it from [LocalGlassBackdrop].
 */
@Stable
class GlassBackdrop internal constructor(
    /** The tier this screen draws its glass with. */
    val tier: GlassTier,
    internal val layer: LayerBackdrop,
)

/** Creates the backdrop for one screen, at the current [GlassTier]. */
@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val tier = currentGlassTier()
    val layer = rememberLayerBackdrop()
    return remember(tier, layer) { GlassBackdrop(tier, layer) }
}

/**
 * Marks the content glass surfaces refract. Recording costs a layer per frame, so at
 * [GlassTier.SOLID] nothing is recorded at all.
 */
fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier =
    if (backdrop.tier == GlassTier.GLASS) layerBackdrop(backdrop.layer) else this

/** The backdrop of the screen being composed; `null` outside a `FunputScreen`. */
val LocalGlassBackdrop: ProvidableCompositionLocal<GlassBackdrop?> = staticCompositionLocalOf { null }
