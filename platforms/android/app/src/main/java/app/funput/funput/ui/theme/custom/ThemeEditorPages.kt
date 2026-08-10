package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.theme.custom.color.ThemeColorLinks
import app.funput.funput.ui.theme.custom.color.ThemeColorList
import app.funput.funput.ui.theme.custom.color.ThemeContrastWarnings
import app.funput.funput.ui.theme.custom.metrics.ThemeGeneralMetrics
import app.funput.funput.ui.theme.custom.metrics.ThemeKeyMetrics
import app.funput.funput.ui.theme.custom.metrics.ThemePressedMetrics

/** The body of one editor page. Each holds about a screenful, which is the point of splitting. */
@Composable
internal fun ThemeEditorPage(
    tab: ThemeEditorTab,
    state: ThemeDraftState,
    baseThemes: List<KeyboardThemeDescriptor>,
    onOpenBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        when (tab) {
            ThemeEditorTab.General -> {
                BaseThemeSelector(
                    baseThemes = baseThemes,
                    selectedValue = state.baseThemeValue,
                    onSelected = state::selectBaseTheme,
                )
                AccentColorSelector(
                    selectedColor = state.theme.accentColor,
                    onSelected = state::applyAccent,
                )
                ThemeGeneralMetrics(state.theme, state::updateTheme)
            }
            ThemeEditorTab.Background -> {
                ThemeColorList(tab, state.theme, state.colorWriter())
                ThemeBackgroundRow(
                    hasImage = state.backgroundImage != null,
                    onClick = onOpenBackground,
                )
            }
            ThemeEditorTab.Keys -> {
                ThemeColorList(tab, state.theme, state.colorWriter())
                ThemeKeyMetrics(state.theme, state::updateTheme)
                ThemeContrastWarnings(state.theme)
            }
            ThemeEditorTab.Pressed -> {
                ThemeColorList(tab, state.theme, state.colorWriter())
                ThemePressedMetrics(state.theme, state::updateTheme)
            }
        }
    }
}

private fun ThemeDraftState.colorWriter(): (app.funput.funput.ui.theme.custom.color.ThemeColorRole, Int) -> Unit =
    { role, color -> updateTheme { theme -> ThemeColorLinks.write(theme, role, color) } }
