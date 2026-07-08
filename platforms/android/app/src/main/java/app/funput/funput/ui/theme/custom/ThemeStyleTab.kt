package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId

@Composable
internal fun ThemeStyleTab(
    baseThemes: List<KeyboardThemeDescriptor>,
    selectedBaseThemeId: KeyboardThemeId,
    accentColor: Int,
    keyBackgroundOpacity: Float,
    onBaseThemeSelected: (KeyboardThemeId) -> Unit,
    onAccentSelected: (Int) -> Unit,
    onKeyBackgroundOpacityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = modifier) {
        BaseThemeSelector(
            themes = baseThemes,
            selectedThemeId = selectedBaseThemeId,
            onSelected = onBaseThemeSelected,
        )
        AccentColorSelector(
            selectedColor = accentColor,
            onSelected = onAccentSelected,
        )
        KeyBackgroundOpacitySelector(
            opacity = keyBackgroundOpacity,
            onOpacityChange = onKeyBackgroundOpacityChange,
        )
    }
}
