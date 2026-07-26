package app.funput.funput.ui.theme.custom.color

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.funput.funput.R

/**
 * Free color picker: hue, saturation and value on a field, plus alpha and a hex box.
 *
 * The dialog edits a private copy so dismissing leaves the theme untouched; only the confirm
 * button hands a color back. Alpha is offered for every role because a theme can hide a surface
 * by making it fully transparent, which is how the plateless dark theme is built.
 */
@Composable
internal fun ColorPickerDialog(
    title: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var state by remember(initialColor) { mutableStateOf(ColorPickerState.from(initialColor)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.argb) }) {
                Text(stringResource(R.string.custom_theme_color_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.custom_theme_color_picker_cancel))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SaturationValueField(
                    hue = state.hue,
                    saturation = state.saturation,
                    value = state.value,
                    onChange = { saturation, value ->
                        state = state.copy(saturation = saturation, value = value)
                    },
                )
                GradientBar(
                    colors = HueColors,
                    position = state.hue / MaxHue,
                    onChange = { position -> state = state.copy(hue = position * MaxHue) },
                )
                GradientBar(
                    colors = listOf(Color.Transparent, Color(state.opaqueArgb)),
                    position = state.alpha,
                    onChange = { position -> state = state.copy(alpha = position) },
                )
                Text(
                    text = stringResource(
                        R.string.custom_theme_color_picker_alpha,
                        (state.alpha * PercentScale).toInt(),
                    ),
                )
                ColorPreview(state.argb)
                HexField(state) { updated -> state = updated }
            }
        },
    )
}

@Composable
private fun ColorPreview(argb: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(argb)),
    ) {}
}

@Composable
private fun HexField(state: ColorPickerState, onChange: (ColorPickerState) -> Unit) {
    var text by remember(state.argb) { mutableStateOf(state.hex) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            ColorPickerState.fromHexOrNull(input, state.alpha)?.let(onChange)
        },
        label = { Text(stringResource(R.string.custom_theme_color_picker_hex)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        modifier = Modifier.fillMaxWidth(),
    )
}

private const val MaxHue = 360f
private const val PercentScale = 100f
