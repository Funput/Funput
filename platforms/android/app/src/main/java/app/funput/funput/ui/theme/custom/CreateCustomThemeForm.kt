package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId

@Composable
internal fun CreateCustomThemeForm(
    name: String,
    baseThemes: List<KeyboardThemeDescriptor>,
    selectedBaseThemeId: KeyboardThemeId,
    accentColor: Int,
    keyBackgroundOpacity: Float,
    backgroundImageSource: String?,
    imageOpacity: Float,
    previewTheme: KeyboardTheme,
    contentPadding: PaddingValues,
    onNameChange: (String) -> Unit,
    onBaseThemeSelected: (KeyboardThemeId) -> Unit,
    onAccentSelected: (Int) -> Unit,
    onKeyBackgroundOpacityChange: (Float) -> Unit,
    onImageOpacityChange: (Float) -> Unit,
    onChooseBackgroundImage: () -> Unit,
    onRemoveBackgroundImage: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(CreateThemeEditorTab.Style) }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "preview") {
            CreateThemePreviewHeader(
                previewTheme = previewTheme,
                backgroundImageSource = backgroundImageSource,
                imageOpacity = imageOpacity,
            )
        }
        item(key = "tabs") {
            CreateThemeEditorTabs(
                selectedTab = selectedTab,
                onSelected = { selectedTab = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = selectedTab.name) {
            CreateThemeTabContent(
                selectedTab = selectedTab,
                name = name,
                baseThemes = baseThemes,
                selectedBaseThemeId = selectedBaseThemeId,
                accentColor = accentColor,
                keyBackgroundOpacity = keyBackgroundOpacity,
                backgroundImageSource = backgroundImageSource,
                imageOpacity = imageOpacity,
                onNameChange = onNameChange,
                onBaseThemeSelected = onBaseThemeSelected,
                onAccentSelected = onAccentSelected,
                onKeyBackgroundOpacityChange = onKeyBackgroundOpacityChange,
                onImageOpacityChange = onImageOpacityChange,
                onChooseBackgroundImage = onChooseBackgroundImage,
                onRemoveBackgroundImage = onRemoveBackgroundImage,
            )
        }
        item(key = "save") {
            Button(
                enabled = name.trim().isNotEmpty(),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.custom_theme_save))
            }
            if (name.trim().isEmpty()) {
                Text(
                    text = stringResource(R.string.custom_theme_save_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun CreateThemeTabContent(
    selectedTab: CreateThemeEditorTab,
    name: String,
    baseThemes: List<KeyboardThemeDescriptor>,
    selectedBaseThemeId: KeyboardThemeId,
    accentColor: Int,
    keyBackgroundOpacity: Float,
    backgroundImageSource: String?,
    imageOpacity: Float,
    onNameChange: (String) -> Unit,
    onBaseThemeSelected: (KeyboardThemeId) -> Unit,
    onAccentSelected: (Int) -> Unit,
    onKeyBackgroundOpacityChange: (Float) -> Unit,
    onImageOpacityChange: (Float) -> Unit,
    onChooseBackgroundImage: () -> Unit,
    onRemoveBackgroundImage: () -> Unit,
) = when (selectedTab) {
    CreateThemeEditorTab.Style -> ThemeStyleTab(
        baseThemes = baseThemes,
        selectedBaseThemeId = selectedBaseThemeId,
        accentColor = accentColor,
        keyBackgroundOpacity = keyBackgroundOpacity,
        onBaseThemeSelected = onBaseThemeSelected,
        onAccentSelected = onAccentSelected,
        onKeyBackgroundOpacityChange = onKeyBackgroundOpacityChange,
    )
    CreateThemeEditorTab.Background -> BackgroundImagePlaceholder(
        imageSelected = backgroundImageSource != null,
        opacity = imageOpacity,
        onOpacityChange = onImageOpacityChange,
        onChooseImage = onChooseBackgroundImage,
        onRemoveImage = onRemoveBackgroundImage,
    )
    CreateThemeEditorTab.Info -> ThemeInfoTab(name = name, onNameChange = onNameChange)
}
