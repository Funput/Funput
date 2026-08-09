package app.funput.funput.ui.theme.custom

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.store.custom.CustomThemeDraft
import app.funput.funput.theme.store.themeAssetStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
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
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        // Copy the bytes in rather than keeping the picker's URI: the grant can be revoked and
        // the user can delete the photo, either of which would leave the theme with no image.
        scope.launch {
            val stored = withContext(Dispatchers.IO) { assetStore.store(context, uri) }
            stored?.let(state::selectBackgroundImage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { ThemeStudioTopBar(state, baseThemes, onBack) },
        bottomBar = {
            ThemeStudioActionBar(
                canSave = state.canSave,
                onSave = { onSave(state.toDraft()) },
                onCancel = onBack,
            )
        },
    ) { padding ->
        CreateCustomThemeForm(
            state = state,
            contentPadding = padding,
            editingThemeId = editingTheme?.id,
            onChooseBackgroundImage = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }
}
