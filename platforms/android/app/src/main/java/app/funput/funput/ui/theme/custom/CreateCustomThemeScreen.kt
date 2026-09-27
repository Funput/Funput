package app.funput.funput.ui.theme.custom

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.store.custom.CustomThemeDraft
import app.funput.funput.theme.store.themeAssetStore
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.layout.FunputEditorScreen
import app.funput.funput.ui.theme.custom.background.ThemeBackgroundScreen
import app.funput.funput.ui.theme.custom.draft.rememberThemeDraftState
import app.funput.funput.ui.theme.custom.studio.ThemeEditorTab
import app.funput.funput.ui.theme.custom.studio.ThemeStudioActionBar
import app.funput.funput.ui.theme.custom.studio.ThemeStudioHeader
import app.funput.funput.ui.theme.custom.studio.ThemeStudioPages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The theme studio: the live keyboard pinned at the top, four pages of controls under it, and save
 * always at hand. [editingTheme] opens an existing custom theme; without it a new one is drafted
 * from [baseThemes]. "Restore" undoes every change back to the theme it started from.
 */
@Composable
internal fun CreateCustomThemeScreen(
    baseThemes: List<KeyboardThemeDescriptor>,
    editingTheme: KeyboardThemeDescriptor? = null,
    onSave: (CustomThemeDraft) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state = rememberThemeDraftState(baseThemes, editingTheme)
    val assetStore = remember(context) { context.themeAssetStore() }
    val scope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        // Copy the bytes in rather than keeping the picker's URI: the grant can be revoked and
        // the user can delete the photo, either of which would leave the theme with no image.
        scope.launch {
            val stored = withContext(Dispatchers.IO) { assetStore.store(context, uri) }
            stored?.let(state::selectBackgroundImage)
        }
    }
    var editingBackground by rememberSaveable { mutableStateOf(false) }
    // Above the branch below, so coming back from the image editor lands on the page it left from.
    val pagerState = rememberPagerState { ThemeEditorTab.entries.size }
    if (editingBackground) {
        // System back leaves the image editor for the studio, not the studio for the gallery.
        BackHandler { editingBackground = false }
        ThemeBackgroundScreen(
            state = state,
            onChooseImage = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onBack = { editingBackground = false },
            modifier = modifier,
        )
        return
    }
    FunputEditorScreen(
        title = stringResource(if (editingTheme != null) R.string.custom_theme_edit_title else R.string.custom_theme_title),
        modifier = modifier,
        onBack = onBack,
        actions = {
            FunputButton(
                text = stringResource(R.string.custom_theme_restore),
                onClick = state::restoreBase,
                style = FunputButtonStyle.PLAIN,
            )
        },
        header = {
            ThemeStudioHeader(
                theme = state.theme,
                backgroundImage = state.backgroundImage,
                editingThemeId = editingTheme?.id,
                selectedTab = ThemeEditorTab.entries[pagerState.targetPage],
                onSelectTab = { tab -> scope.launch { pagerState.animateScrollToPage(tab.ordinal) } },
            )
        },
        bottomBar = {
            ThemeStudioActionBar(canSave = state.canSave, onSave = { onSave(state.toDraft()) }, onCancel = onBack)
        },
    ) {
        ThemeStudioPages(pagerState, state, baseThemes, onOpenBackground = { editingBackground = true })
    }
}
