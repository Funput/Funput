package app.funput.funput.ui.theme.custom

import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R

internal enum class CreateThemeEditorTab(val titleRes: Int) {
    Style(R.string.custom_theme_tab_style),
    Colors(R.string.custom_theme_tab_colors),
    Metrics(R.string.custom_theme_tab_metrics),
    Background(R.string.custom_theme_tab_background),
    Info(R.string.custom_theme_tab_info),
}

@Composable
internal fun CreateThemeEditorTabs(
    selectedTab: CreateThemeEditorTab,
    onSelected: (CreateThemeEditorTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    PrimaryTabRow(selectedTabIndex = CreateThemeEditorTab.entries.indexOf(selectedTab), modifier = modifier) {
        CreateThemeEditorTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onSelected(tab) },
                text = { Text(stringResource(tab.titleRes)) },
            )
        }
    }
}
