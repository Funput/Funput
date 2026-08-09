package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.funput.funput.theme.KeyboardThemeId

/**
 * Pinned preview above, scrolling controls below.
 *
 * The preview used to be the first item of the scrolling list, which meant that by the time you
 * reached the control you wanted to change, the keyboard you were changing had scrolled off the
 * screen. Keeping it out of the scroll is the whole point of a live preview.
 */
@Composable
internal fun CreateCustomThemeForm(
    state: ThemeDraftState,
    contentPadding: PaddingValues,
    editingThemeId: KeyboardThemeId?,
    onChooseBackgroundImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(CreateThemeEditorTab.Colors) }

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
        CreateThemeEditorTabs(
            selectedTab = selectedTab,
            onSelected = { selectedTab = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            CreateThemeTabContent(
                selectedTab = selectedTab,
                state = state,
                onChooseBackgroundImage = onChooseBackgroundImage,
            )
        }
    }
}
