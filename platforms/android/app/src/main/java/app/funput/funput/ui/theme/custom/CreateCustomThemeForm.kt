package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId

/**
 * Pinned preview above, one page of controls below.
 *
 * The preview used to be the first item of a scrolling list, which meant that by the time you
 * reached the control you wanted, the keyboard you were changing had scrolled off the screen.
 * Keeping it out of the scroll is the whole point of a live preview.
 */
@Composable
internal fun CreateCustomThemeForm(
    state: ThemeDraftState,
    contentPadding: PaddingValues,
    editingThemeId: KeyboardThemeId?,
    baseThemes: List<KeyboardThemeDescriptor>,
    onOpenBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        ThemeStudioPreview(
            theme = state.theme,
            backgroundImage = state.backgroundImage,
            editingThemeId = editingThemeId,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
        ThemeEditorPager(
            state = state,
            baseThemes = baseThemes,
            onOpenBackground = onOpenBackground,
        )
    }
}
