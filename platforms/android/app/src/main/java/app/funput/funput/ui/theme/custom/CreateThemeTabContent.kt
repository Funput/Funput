package app.funput.funput.ui.theme.custom

import androidx.compose.runtime.Composable
import app.funput.funput.ui.theme.custom.background.ThemeBackgroundTab
import app.funput.funput.ui.theme.custom.color.ThemeColorTab
import app.funput.funput.ui.theme.custom.metrics.ThemeMetricsTab

/**
 * Routes the selected editor tab to its controls.
 *
 * Tabs read and write [ThemeDraftState] directly, so a new control lands in one tab file and
 * nowhere else.
 */
@Composable
internal fun CreateThemeTabContent(
    selectedTab: CreateThemeEditorTab,
    state: ThemeDraftState,
    onChooseBackgroundImage: () -> Unit,
) = when (selectedTab) {
    CreateThemeEditorTab.Colors -> ThemeColorTab(
        theme = state.theme,
        onColorChange = { role, color ->
            state.updateTheme { theme -> role.write(theme, color) }
        },
        accentShortcut = {
            // The accent presets stay at the top of the color list as a one-tap starting point;
            // they write the whole accent family, which the individual role pickers do not.
            AccentColorSelector(
                selectedColor = state.theme.accentColor,
                onSelected = { color -> state.updateTheme { theme -> theme.withAccent(color) } },
            )
        },
    )
    CreateThemeEditorTab.Metrics -> ThemeMetricsTab(
        theme = state.theme,
        onThemeChange = { transform -> state.updateTheme(transform) },
    )
    CreateThemeEditorTab.Background -> ThemeBackgroundTab(
        state = state,
        onChooseImage = onChooseBackgroundImage,
    )
}
