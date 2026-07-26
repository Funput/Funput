package app.funput.funput.ui.theme.custom.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.ui.theme.custom.ThemeDraftState
import app.funput.funput.ui.theme.custom.color.ColorPickerDialog
import app.funput.funput.ui.theme.custom.color.ColorSwatchRow
import app.funput.funput.ui.theme.custom.metrics.ThemeDpSlider
import app.funput.funput.ui.theme.custom.metrics.ThemePercentSlider

@Composable
internal fun ThemeBackgroundTab(
    state: ThemeDraftState,
    onChooseImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val image = state.backgroundImage
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier) {
        if (image == null) {
            Text(
                text = stringResource(R.string.custom_theme_background_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onChooseImage) {
                Text(stringResource(R.string.custom_theme_background_choose))
            }
        } else {
            ChosenImageControls(state, image, onChooseImage)
        }
    }
}

@Composable
private fun ChosenImageControls(
    state: ThemeDraftState,
    image: KeyboardThemeBackgroundImage,
    onChooseImage: () -> Unit,
) {
    var editingOverlay by remember { mutableStateOf(false) }

    BackgroundFocusPicker(
        source = image.source,
        focalX = image.focalX,
        focalY = image.focalY,
        onFocusChange = { x, y ->
            state.updateBackgroundImage { it.copy(focalX = x, focalY = y) }
        },
    )
    Text(
        text = stringResource(R.string.custom_theme_background_focus_hint),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onChooseImage) {
            Text(stringResource(R.string.custom_theme_background_change))
        }
        TextButton(onClick = { state.backgroundImage = null }) {
            Text(stringResource(R.string.custom_theme_background_remove))
        }
    }
    ThemePercentSlider(
        label = stringResource(R.string.custom_theme_background_opacity_label),
        value = image.opacity,
        range = 0f..1f,
        onChange = { value -> state.updateBackgroundImage { it.copy(opacity = value) } },
    )
    ThemeDpSlider(
        label = stringResource(R.string.custom_theme_background_zoom_label),
        value = image.zoom,
        range = KeyboardThemeBackgroundImage.ZoomRange,
        onChange = { value -> state.updateBackgroundImage { it.copy(zoom = value) } },
    )
    ThemeDpSlider(
        label = stringResource(R.string.custom_theme_background_blur_label),
        value = image.blurRadiusDp,
        range = KeyboardThemeBackgroundImage.BlurRange,
        onChange = { value -> state.updateBackgroundImage { it.copy(blurRadiusDp = value) } },
    )
    ColorSwatchRow(
        label = stringResource(R.string.custom_theme_background_overlay),
        color = image.overlayColor,
        onClick = { editingOverlay = true },
    )

    if (editingOverlay) {
        ColorPickerDialog(
            title = stringResource(R.string.custom_theme_background_overlay),
            initialColor = image.overlayColor,
            onDismiss = { editingOverlay = false },
            onConfirm = { color ->
                state.updateBackgroundImage { it.copy(overlayColor = color) }
                editingOverlay = false
            },
        )
    }
}
