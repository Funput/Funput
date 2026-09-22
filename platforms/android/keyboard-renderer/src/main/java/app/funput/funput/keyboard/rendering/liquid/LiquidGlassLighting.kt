package app.funput.funput.keyboard.rendering.liquid

import app.funput.funput.theme.KeyboardKeySurfaceStyle
import app.funput.funput.theme.KeyboardTheme

/** The axis a ramp runs along, expressed in the unit square so one shader fits any key. */
internal enum class LiquidGlassAxis(val endX: Float, val endY: Float) {
    /** Top edge to bottom edge: the light a pane of glass gathers along its lip. */
    VERTICAL(0f, 1f),

    /** Top-left to bottom-right: the two corners a lens throws its specular back from. */
    DIAGONAL(1f, 1f),
}

/** Color stops for one Liquid Glass pass, in unit space along [axis]. */
internal class LiquidGlassRamp(
    val colors: IntArray,
    val positions: FloatArray,
    val axis: LiquidGlassAxis,
)

/**
 * The light model behind the Liquid Glass surface.
 *
 * Apple's material is achromatic: a key is a clear pane that brightens where light enters its
 * edge, not a colored plate. The ramps below are therefore fixed whites rather than theme tints,
 * so a Liquid Glass theme reads as glass over whatever ground it is authored against. Only the
 * surface style gates them; every other theme gets no ramp and pays nothing to draw.
 */
internal object LiquidGlassLighting {
    /** Outline that keeps a clear key from dissolving into the ground behind it. */
    const val Contour = 0x1A000000

    /**
     * The body of the pane: a thin catch along the top, clear through the middle, and a wider
     * gather at the base where light leaving the glass piles up.
     */
    fun body(theme: KeyboardTheme): LiquidGlassRamp? = ramp(theme) {
        LiquidGlassRamp(
            colors = intArrayOf(BodyCrown, Clear, Clear, BodyLip),
            positions = floatArrayOf(0f, BodyCrownEnd, BodyLipStart, 1f),
            axis = LiquidGlassAxis.VERTICAL,
        )
    }

    /** The same pane while held, lit as though the finger pushed it into the light. */
    fun pressedBody(theme: KeyboardTheme): LiquidGlassRamp? = ramp(theme) {
        LiquidGlassRamp(
            colors = intArrayOf(PressedCrown, PressedMiddle, PressedLip),
            positions = floatArrayOf(0f, PressedMiddleStop, 1f),
            axis = LiquidGlassAxis.VERTICAL,
        )
    }

    /**
     * The rim, which is what actually reads as glass.
     *
     * A lens is brightest where it faces the light and where light exits the far side, so the
     * stroke peaks at the top-left and bottom-right and nearly vanishes along the run between
     * them. An evenly bright outline would read as a stroked button instead.
     */
    fun rim(theme: KeyboardTheme): LiquidGlassRamp? = ramp(theme) {
        LiquidGlassRamp(
            colors = intArrayOf(RimEntry, RimShoulder, RimShoulder, RimExit),
            positions = floatArrayOf(0f, RimShoulderStart, RimShoulderEnd, 1f),
            axis = LiquidGlassAxis.DIAGONAL,
        )
    }

    /** Same rim, brightened so a held key keeps its edge against the lit body. */
    fun pressedRim(theme: KeyboardTheme): LiquidGlassRamp? = ramp(theme) {
        LiquidGlassRamp(
            colors = intArrayOf(PressedRimEntry, PressedRimShoulder, PressedRimShoulder, PressedRimExit),
            positions = floatArrayOf(0f, RimShoulderStart, RimShoulderEnd, 1f),
            axis = LiquidGlassAxis.DIAGONAL,
        )
    }

    private inline fun ramp(theme: KeyboardTheme, build: () -> LiquidGlassRamp): LiquidGlassRamp? =
        if (theme.keySurfaceStyle == KeyboardKeySurfaceStyle.LIQUID_GLASS) build() else null

    private const val Clear = 0x00FFFFFF
    private const val BodyCrown = 0x14FFFFFF
    private const val BodyLip = 0x12FFFFFF
    private const val BodyCrownEnd = 0.1f
    private const val BodyLipStart = 0.88f
    private const val PressedCrown = 0x2EFFFFFF
    private const val PressedMiddle = 0x0AFFFFFF
    private const val PressedLip = 0x24FFFFFF
    private const val PressedMiddleStop = 0.5f
    private const val RimEntry = 0x47FFFFFF
    private const val RimShoulder = 0x0AFFFFFF
    private const val RimExit = 0x2EFFFFFF
    private const val RimShoulderStart = 0.24f
    private const val RimShoulderEnd = 0.66f
    private const val PressedRimEntry = 0x80FFFFFF.toInt()
    private const val PressedRimShoulder = 0x1FFFFFFF
    private const val PressedRimExit = 0x52FFFFFF
}
