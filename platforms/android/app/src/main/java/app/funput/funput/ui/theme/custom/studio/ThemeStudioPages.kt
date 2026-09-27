package app.funput.funput.ui.theme.custom.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.widthIn
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.rows.LinkRow
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.theme.custom.ThemeDraftState
import app.funput.funput.ui.theme.custom.color.ThemeColorLinks
import app.funput.funput.ui.theme.custom.color.ThemeColorList
import app.funput.funput.ui.theme.custom.color.ThemeColorRole
import app.funput.funput.ui.theme.custom.color.ThemeContrastWarnings
import app.funput.funput.ui.theme.custom.metrics.ThemePressedSection
import app.funput.funput.ui.theme.custom.metrics.ThemeSurfaceSection

/** The editor's pages side by side: tap a tab in the header or swipe to the next page. */
@Composable
internal fun ThemeStudioPages(
    pagerState: PagerState,
    state: ThemeDraftState,
    baseThemes: List<KeyboardThemeDescriptor>,
    onOpenBackground: () -> Unit,
) {
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.section),
                modifier = Modifier
                    .widthIn(max = FunputUi.spacing.contentMaxWidth)
                    .padding(horizontal = FunputUi.spacing.pageMargin, vertical = FunputUi.spacing.medium),
            ) {
                ThemeEditorPage(ThemeEditorTab.entries[page], state, baseThemes, onOpenBackground)
            }
        }
    }
}

@Composable
private fun ThemeEditorPage(
    tab: ThemeEditorTab,
    state: ThemeDraftState,
    baseThemes: List<KeyboardThemeDescriptor>,
    onOpenBackground: () -> Unit,
) {
    val writeColor = { role: ThemeColorRole, color: Int ->
        state.updateTheme { theme -> ThemeColorLinks.write(theme, role, color) }
    }
    when (tab) {
        ThemeEditorTab.General -> ThemeGeneralPage(state, baseThemes)
        ThemeEditorTab.Background -> {
            ThemeColorList(tab, state.theme, writeColor)
            FunputSection(title = null) {
                LinkRow(
                    title = stringResource(R.string.custom_theme_background_row),
                    value = stringResource(
                        if (state.backgroundImage != null) R.string.custom_theme_background_set else R.string.custom_theme_background_none,
                    ),
                    onClick = onOpenBackground,
                )
            }
        }
        ThemeEditorTab.Keys -> {
            ThemeContrastWarnings(state.theme)
            ThemeColorList(tab, state.theme, writeColor)
            ThemeSurfaceSection(state.theme, state::updateTheme)
        }
        ThemeEditorTab.Pressed -> {
            ThemeColorList(tab, state.theme, writeColor)
            ThemePressedSection(state.theme, state::updateTheme)
        }
    }
}
