package app.funput.funput.ui.theme

import androidx.compose.material3.ColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrast gate for the brand schemes, mirroring the one keyboard themes already pass through
 * `ThemeValidator`. It covers the pairs the settings screens actually put on top of each other —
 * including section headers, which the previous hand-picked palette failed at 2.2:1.
 */
class FunputContrastTest {

    @Test
    fun `light scheme meets WCAG AA for text`() {
        assertTextContrast(FunputLightColors, "light")
    }

    @Test
    fun `dark scheme meets WCAG AA for text`() {
        assertTextContrast(FunputDarkColors, "dark")
    }

    private fun assertTextContrast(scheme: ColorScheme, name: String) {
        textPairs(scheme).forEach { (label, pair) ->
            val ratio = contrastRatio(pair.first, pair.second)
            assertTrue(
                "$name: $label is ${"%.2f".format(ratio)}:1, below the ${MinimumRatio}:1 minimum",
                ratio >= MinimumRatio,
            )
        }
    }

    private fun textPairs(scheme: ColorScheme) = with(scheme) {
        mapOf(
            "onSurface on surface" to (onSurface to surface),
            "onSurface on surfaceContainer" to (onSurface to surfaceContainer),
            "onSurface on surfaceContainerHigh" to (onSurface to surfaceContainerHigh),
            "onSurfaceVariant on surfaceContainer" to (onSurfaceVariant to surfaceContainer),
            "primary on surface" to (primary to surface),
            "primary on surfaceContainer" to (primary to surfaceContainer),
            "onPrimary on primary" to (onPrimary to primary),
            "onPrimaryContainer on primaryContainer" to (onPrimaryContainer to primaryContainer),
            "onSecondaryContainer on secondaryContainer" to
                (onSecondaryContainer to secondaryContainer),
            "onTertiaryContainer on tertiaryContainer" to
                (onTertiaryContainer to tertiaryContainer),
            "onError on error" to (onError to error),
            "onErrorContainer on errorContainer" to (onErrorContainer to errorContainer),
            "inverseOnSurface on inverseSurface" to (inverseOnSurface to inverseSurface),
        )
    }

    private companion object {
        /** WCAG 2.1 AA for body text. Settings rows are 14–16sp, so the large-text 3:1 never applies. */
        const val MinimumRatio = 4.5
    }
}
