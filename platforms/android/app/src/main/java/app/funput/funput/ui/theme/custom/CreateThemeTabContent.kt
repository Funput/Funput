package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.funput.funput.ui.theme.custom.color.ThemeColorLinks
import app.funput.funput.ui.theme.custom.color.ThemeColorTab
import app.funput.funput.ui.theme.custom.metrics.ThemeMetricsTab

/**
 * The advanced controls, as one list rather than three tabs.
 *
 * Tabs inside a collapsed section inside a scrolling page was three levels of nesting for two
 * kinds of control; the third tab was the background image, which is a task with its own flow and
 * now has its own screen.
 */
@Composable
internal fun ThemeAdvancedControls(
    state: ThemeDraftState,
    onOpenBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = modifier) {
        ThemeMetricsTab(
            theme = state.theme,
            onThemeChange = { transform -> state.updateTheme(transform) },
        )
        ThemeColorTab(
            theme = state.theme,
            onColorChange = { role, color ->
                state.updateTheme { theme -> ThemeColorLinks.write(theme, role, color) }
            },
        )
        ThemeBackgroundRow(
            hasImage = state.backgroundImage != null,
            onClick = onOpenBackground,
        )
    }
}
