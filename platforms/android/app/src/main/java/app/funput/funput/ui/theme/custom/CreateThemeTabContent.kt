package app.funput.funput.ui.theme.custom

import androidx.compose.runtime.Composable
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.theme.custom.color.ThemeColorTab
import app.funput.funput.ui.theme.custom.metrics.ThemeMetricsTab

/**
 * Routes the selected editor tab to its card stack.
 *
 * Tabs read and write [ThemeDraftState] directly, so a new control lands in one tab file and
 * nowhere else. Keeping the switch here rather than inside the form means neither this file nor
 * the form grows a parameter per knob.
 */
@Composable
internal fun CreateThemeTabContent(
    selectedTab: CreateThemeEditorTab,
    state: ThemeDraftState,
    baseThemes: List<KeyboardThemeDescriptor>,
    onChooseBackgroundImage: () -> Unit,
) = when (selectedTab) {
    CreateThemeEditorTab.Style -> ThemeStyleTab(
        state = state,
        baseThemes = baseThemes,
    )
    CreateThemeEditorTab.Colors -> ThemeColorTab(
        theme = state.theme,
        onColorChange = { role, color ->
            state.updateTheme { theme -> role.write(theme, color) }
        },
    )
    CreateThemeEditorTab.Metrics -> ThemeMetricsTab(
        theme = state.theme,
        onThemeChange = { transform -> state.updateTheme(transform) },
    )
    CreateThemeEditorTab.Background -> BackgroundImagePlaceholder(
        imageSelected = state.backgroundImageSource != null,
        opacity = state.imageOpacity,
        onOpacityChange = { opacity -> state.imageOpacity = opacity },
        onChooseImage = onChooseBackgroundImage,
        onRemoveImage = { state.backgroundImageSource = null },
    )
    CreateThemeEditorTab.Info -> ThemeInfoTab(
        name = state.name,
        onNameChange = { name -> state.name = name },
    )
}
