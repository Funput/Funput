package app.funput.funput.ui.theme.custom.background

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.layout.FunputEditorScreen
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.theme.custom.draft.ThemeDraftState
import app.funput.funput.ui.theme.custom.studio.ThemeEditorColumn
import app.funput.funput.ui.theme.custom.studio.ThemeLivePreview

/**
 * Choosing and framing the background image, with the keyboard it applies to pinned above so every
 * drag and slider shows its result. The image is a task of its own (choose, frame, blend), so it
 * gets a screen rather than a page among the colours.
 */
@Composable
internal fun ThemeBackgroundScreen(
    state: ThemeDraftState,
    onChooseImage: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FunputEditorScreen(
        title = stringResource(R.string.custom_theme_background_row),
        modifier = modifier,
        onBack = onBack,
        header = { PinnedPreview(state) },
    ) {
        ThemeEditorColumn { ThemeBackgroundSections(state, onChooseImage) }
    }
}

@Composable
private fun PinnedPreview(state: ThemeDraftState) {
    Box(
        Modifier.padding(
            start = FunputUi.spacing.pageMargin,
            end = FunputUi.spacing.pageMargin,
            bottom = FunputUi.spacing.medium,
        ),
    ) {
        ThemeLivePreview(state.theme, state.backgroundImage, editingThemeId = null)
    }
}
