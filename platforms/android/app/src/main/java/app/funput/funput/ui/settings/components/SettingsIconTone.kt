package app.funput.funput.ui.settings.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * How a settings icon is tinted.
 *
 * The tiles used to be the four brand colours at full saturation — eight of them down a page, and
 * the only colour on it. They came from the earlier iOS-shaped design and they were the one thing
 * left that did not belong to the colour scheme: they could not follow a wallpaper palette, and
 * next to the calm surfaces around them they read as louder than anything they labelled.
 *
 * Three tones from the scheme keep rows apart at a glance while belonging to it. The pairing is
 * resolved here rather than at call sites, so a tile cannot end up with a glyph nobody can read.
 */
@Immutable
internal enum class SettingsIconTone {
    Primary,
    Secondary,
    Tertiary,
    ;

    val container: Color
        @Composable get() = when (this) {
            Primary -> MaterialTheme.colorScheme.primaryContainer
            Secondary -> MaterialTheme.colorScheme.secondaryContainer
            Tertiary -> MaterialTheme.colorScheme.tertiaryContainer
        }

    val content: Color
        @Composable get() = when (this) {
            Primary -> MaterialTheme.colorScheme.onPrimaryContainer
            Secondary -> MaterialTheme.colorScheme.onSecondaryContainer
            Tertiary -> MaterialTheme.colorScheme.onTertiaryContainer
        }
}
