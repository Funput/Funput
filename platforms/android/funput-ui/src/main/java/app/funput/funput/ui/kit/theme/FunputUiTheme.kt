package app.funput.funput.ui.kit.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import app.funput.funput.ui.kit.theme.material.MaterialBridge

private val LocalFunputColors = staticCompositionLocalOf { FunputColors.of(isDark = false) }
private val LocalFunputTypography = staticCompositionLocalOf { FunputTypography.Default }

/**
 * Applies FunputUI to [content]: colours for [isDark], the type scale, and a matching Material
 * theme so the few borrowed Material components (sheet, dialog, slider) look like the rest.
 */
@Composable
fun FunputUiTheme(isDark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = remember(isDark) { FunputColors.of(isDark) }
    CompositionLocalProvider(
        LocalFunputColors provides colors,
        LocalFunputTypography provides FunputTypography.Default,
    ) {
        MaterialBridge(colors, FunputTypography.Default, content)
    }
}

/** How much a disabled control or row is dimmed; one value so every disabled thing reads alike. */
internal const val DisabledAlpha: Float = 0.4f

/** Entry point to the current FunputUI theme, like `MaterialTheme` is for Material. */
object FunputUi {
    /** Colour roles for the current appearance. */
    val colors: FunputColors
        @Composable @ReadOnlyComposable get() = LocalFunputColors.current

    /** The type scale. */
    val typography: FunputTypography
        @Composable @ReadOnlyComposable get() = LocalFunputTypography.current

    /** Corner shapes. */
    val shapes: FunputShapes get() = FunputShapes

    /** Spacing and layout measures. */
    val spacing: FunputSpacing get() = FunputSpacing

    /** Named animations. */
    val motion: FunputMotion get() = FunputMotion
}
