package app.funput.funput.ui.theme.custom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.theme.KeyboardThemePreview

@Composable
internal fun CreateCustomThemeForm(
    name: String,
    baseThemes: List<KeyboardThemeDescriptor>,
    selectedBaseThemeId: KeyboardThemeId,
    accentColor: Int,
    imageOpacity: Float,
    previewTheme: KeyboardTheme,
    contentPadding: PaddingValues,
    onNameChange: (String) -> Unit,
    onBaseThemeSelected: (KeyboardThemeId) -> Unit,
    onAccentSelected: (Int) -> Unit,
    onImageOpacityChange: (Float) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "preview") {
            Text(
                text = stringResource(R.string.custom_theme_preview_title),
                style = MaterialTheme.typography.titleMedium,
            )
            KeyboardThemePreview(
                theme = previewTheme,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(CardShape),
            )
        }
        item(key = "name") {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                label = { Text(stringResource(R.string.custom_theme_name_label)) },
                placeholder = { Text(stringResource(R.string.custom_theme_name_placeholder)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom-theme-name"),
            )
        }
        item(key = "base") {
            BaseThemeSelector(
                themes = baseThemes,
                selectedThemeId = selectedBaseThemeId,
                onSelected = onBaseThemeSelected,
            )
        }
        item(key = "accent") {
            AccentColorSelector(
                selectedColor = accentColor,
                onSelected = onAccentSelected,
            )
        }
        item(key = "background") {
            BackgroundImagePlaceholder(
                opacity = imageOpacity,
                onOpacityChange = onImageOpacityChange,
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
        }
    }
}
