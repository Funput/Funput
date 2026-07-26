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
import app.funput.funput.theme.KeyboardThemeDescriptor

@Composable
internal fun CreateCustomThemeForm(
    state: ThemeDraftState,
    baseThemes: List<KeyboardThemeDescriptor>,
    contentPadding: PaddingValues,
    onChooseBackgroundImage: () -> Unit,
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
                previewTheme = state.theme,
                backgroundImage = state.backgroundImage,
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
                state = state,
                baseThemes = baseThemes,
                onChooseBackgroundImage = onChooseBackgroundImage,
            )
        }
        item(key = "save") {
            Button(
                enabled = state.canSave,
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.custom_theme_save))
            }
            if (!state.canSave) {
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
