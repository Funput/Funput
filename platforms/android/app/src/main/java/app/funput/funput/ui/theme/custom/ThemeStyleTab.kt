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
            onSelected = { id -> state.selectBaseTheme(id.value) },
        )
        AccentColorSelector(
            selectedColor = state.theme.accentColor,
            onSelected = { color -> state.updateTheme { theme -> theme.withAccent(color) } },
        )
    }
}
