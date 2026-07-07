package app.funput.funput.ui.theme.custom

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.store.custom.CustomThemeDraft
import app.funput.funput.theme.store.custom.CustomThemeOverrides

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateCustomThemeScreen(
    baseThemes: List<KeyboardThemeDescriptor>,
    onSave: (CustomThemeDraft) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var baseThemeValue by rememberSaveable { mutableStateOf(KeyboardThemeId.Default.value) }
    var accentColor by rememberSaveable { mutableStateOf(AccentPresets.first().argb) }
    var imageOpacity by rememberSaveable { mutableFloatStateOf(0.4f) }
    val fallbackTheme = baseThemes.first()
    val baseTheme = baseThemes.find { theme -> theme.id.value == baseThemeValue } ?: fallbackTheme
    val previewTheme = remember(baseTheme, accentColor) {
        CustomThemeOverrides(accentColor = accentColor).applyTo(baseTheme.theme)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.custom_theme_title)) },
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
            name = name,
            baseThemes = baseThemes,
            selectedBaseThemeId = baseTheme.id,
            accentColor = accentColor,
            imageOpacity = imageOpacity,
            previewTheme = previewTheme,
            contentPadding = padding,
            onNameChange = { name = it },
            onBaseThemeSelected = { id -> baseThemeValue = id.value },
            onAccentSelected = { color -> accentColor = color },
            onImageOpacityChange = { opacity -> imageOpacity = opacity },
            onSave = {
                onSave(
                    CustomThemeDraft(
                        name = name,
                        baseThemeId = baseTheme.id,
                        overrides = CustomThemeOverrides(accentColor = accentColor),
                    ),
                )
            },
        )
    }
}
