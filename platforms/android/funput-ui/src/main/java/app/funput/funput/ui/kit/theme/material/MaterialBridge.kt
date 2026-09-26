package app.funput.funput.ui.kit.theme.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.funput.funput.ui.kit.theme.FunputColors
import app.funput.funput.ui.kit.theme.FunputTypography

/**
 * Maps FunputUI onto a Material theme.
 *
 * FunputUI borrows a handful of Material components rather than rebuilding them (the bottom
 * sheet, the dialog, the slider). They read `MaterialTheme`, so it is filled from the same
 * tokens: Funput orange as primary, the card colour as every surface, the Funput type scale.
 * Nothing outside FunputUI should read `MaterialTheme` directly.
 */
@Composable
internal fun MaterialBridge(
    colors: FunputColors,
    typography: FunputTypography,
    content: @Composable () -> Unit,
) {
    val scheme = remember(colors) {
        val base = if (colors.isDark) darkColorScheme() else lightColorScheme()
        base.copy(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            background = colors.groupedBackground,
            onBackground = colors.label,
            surface = colors.cardBackground,
            onSurface = colors.label,
            onSurfaceVariant = colors.secondaryLabel,
            surfaceContainerLow = colors.cardBackground,
            surfaceContainer = colors.cardBackground,
            surfaceContainerHigh = colors.cardBackground,
            surfaceContainerHighest = colors.cardBackground,
            outline = colors.separator,
            outlineVariant = colors.cardStroke,
            error = colors.destructive,
        )
    }
    val type = remember(typography) {
        Typography(
            headlineSmall = typography.title,
            titleLarge = typography.title,
            titleMedium = typography.headline,
            bodyLarge = typography.body,
            bodyMedium = typography.body,
            bodySmall = typography.caption,
            labelLarge = typography.label,
            labelMedium = typography.label,
            labelSmall = typography.caption,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
