package app.funput.funput.ui.theme.custom

import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R

/**
 * The editor's three sections.
 *
 * There were five. The theme name moved to the title bar and the base theme became a restore
 * action, because neither is something you flip between while designing — and at five, the labels
 * wrapped onto two lines and the row read as broken.
 */
internal enum class CreateThemeEditorTab(val titleRes: Int) {
    Colors(R.string.custom_theme_tab_colors),
    Metrics(R.string.custom_theme_tab_metrics),
    Background(R.string.custom_theme_tab_background),
}

@Composable
internal fun CreateThemeEditorTabs(
    selectedTab: CreateThemeEditorTab,
    onSelected: (CreateThemeEditorTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryTabRow(
        selectedTabIndex = CreateThemeEditorTab.entries.indexOf(selectedTab),
        modifier = modifier,
    ) {
        CreateThemeEditorTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onSelected(tab) },
                text = { Text(stringResource(tab.titleRes)) },
            )
        }
    }
}
