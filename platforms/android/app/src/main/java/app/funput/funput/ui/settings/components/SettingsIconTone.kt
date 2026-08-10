package app.funput.funput.ui.settings.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The tint every settings icon carries.
 *
 * There were three, and before that four brand colours. Both were more voices than a settings page
 * has things to say: colour that does not distinguish anything is decoration, and eight decorated
 * squares down a page are what a reader has to look past to find a label. One accent, used
 * everywhere it appears, is what the app it was measured against does.
 */
internal object SettingsIconTone {
    val Container: Color
        @Composable get() = MaterialTheme.colorScheme.primaryContainer

    val Content: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer
}
