package app.funput.funput.ui.theme.custom.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.kit.controls.FunputSegmented
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.navigation.sharedElementByKey
import app.funput.funput.ui.theme.KeyboardThemePreview
import app.funput.funput.ui.theme.themePreviewSharedKey

/**
 * What stays put while the pages scroll: the live keyboard, with nothing around it, and the page
 * switcher. A preview that scrolls away is no preview, so it never leaves the screen.
 */
@Composable
internal fun ThemeStudioHeader(
    theme: KeyboardTheme,
    backgroundImage: KeyboardThemeBackgroundImage?,
    editingThemeId: KeyboardThemeId?,
    selectedTab: ThemeEditorTab,
    onSelectTab: (ThemeEditorTab) -> Unit,
) {
    val tabs = ThemeEditorTab.entries
    Column(
        verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.medium),
        modifier = Modifier.padding(
            start = FunputUi.spacing.pageMargin,
            end = FunputUi.spacing.pageMargin,
            bottom = FunputUi.spacing.medium,
        ),
    ) {
        ThemeLivePreview(theme, backgroundImage, editingThemeId)
        FunputSegmented(
            options = tabs.map { stringResource(it.titleRes) },
            selectedIndex = tabs.indexOf(selectedTab),
            onSelect = { index -> onSelectTab(tabs[index]) },
        )
    }
}

/**
 * The keyboard being edited, drawn by the real renderer at full width. [editingThemeId] ties it to
 * the gallery card it grew from, so opening a theme animates rather than cuts.
 */
@Composable
internal fun ThemeLivePreview(
    theme: KeyboardTheme,
    backgroundImage: KeyboardThemeBackgroundImage?,
    editingThemeId: KeyboardThemeId?,
) {
    KeyboardThemePreview(
        theme = theme,
        backgroundImage = backgroundImage,
        modifier = Modifier
            .sharedElementByKey(themePreviewSharedKey(editingThemeId))
            .fillMaxWidth()
            .height(PreviewHeight)
            .clip(FunputUi.shapes.thumbnail),
    )
}

private val PreviewHeight = 178.dp
