package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.funput.funput.theme.KeyboardThemeDescriptor

@Composable
internal fun ThemeStyleTab(
    state: ThemeDraftState,
    baseThemes: List<KeyboardThemeDescriptor>,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = modifier) {
        BaseThemeSelector(
            themes = baseThemes,
            selectedThemeId = state.baseTheme.id,
            onSelected = { id -> state.baseThemeValue = id.value },
        )
        AccentColorSelector(
            selectedColor = state.accentColor,
            onSelected = { color -> state.accentColor = color },
        )
        KeyBackgroundOpacitySelector(
            opacity = state.keyBackgroundOpacity,
            onOpacityChange = { opacity -> state.keyBackgroundOpacity = opacity },
        )
    }
}
