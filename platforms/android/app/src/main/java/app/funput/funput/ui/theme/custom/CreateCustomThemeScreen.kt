package app.funput.funput.ui.theme.custom

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.store.custom.CustomThemeDraft

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
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            context.persistBackgroundImageAccess(it)
            state.backgroundImageSource = it.toString()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (editingTheme == null) R.string.custom_theme_title else R.string.custom_theme_edit_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.custom_theme_back_description),
                        )
                    }
                },
            )
        },
    ) { padding ->
        CreateCustomThemeForm(
            state = state,
            baseThemes = baseThemes,
            contentPadding = padding,
            onChooseBackgroundImage = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onSave = { onSave(state.toDraft()) },
        )
    }
}
