package app.funput.funput.ui.theme.custom.color.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.controls.FunputTextField
import app.funput.funput.ui.kit.overlays.FunputSheet
import app.funput.funput.ui.kit.theme.FunputUi

/**
 * Free colour picker: saturation and value on a field, hue and opacity on bars, and a hex box.
 *
 * It edits a private copy, so dismissing leaves the theme untouched; only [onConfirm] hands a colour
 * back. Opacity is offered for every role because a theme can hide a surface by making it fully
 * transparent, which is how the plateless dark theme is built. The sheet does not follow the
 * finger down: every drag on it is a colour being chosen, not a request to close.
 */
@Composable
internal fun ColorPickerSheet(
    title: String,
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var state by remember(initialColor) { mutableStateOf(ColorPickerState.from(initialColor)) }
    FunputSheet(onDismiss = onDismiss, title = title, gesturesEnabled = false) {
        SaturationValueField(
            hue = state.hue,
            saturation = state.saturation,
            value = state.value,
            onChange = { saturation, value -> state = state.copy(saturation = saturation, value = value) },
        )
        GradientBar(
            colors = HueColors,
            position = state.hue / MaxHue,
            onChange = { position -> state = state.copy(hue = position * MaxHue) },
        )
        Column(verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.tight)) {
            GradientBar(
                colors = listOf(Color.Transparent, Color(state.opaqueArgb)),
                position = state.alpha,
                onChange = { position -> state = state.copy(alpha = position) },
            )
            BasicText(
                text = stringResource(R.string.custom_theme_color_picker_alpha, (state.alpha * PercentScale).toInt()),
                style = FunputUi.typography.caption.copy(color = FunputUi.colors.secondaryLabel),
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(FunputUi.spacing.medium),
        ) {
            HexField(state, Modifier.weight(1f)) { updated -> state = updated }
            ColorPreview(state.argb)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FunputUi.spacing.medium)) {
            FunputButton(
                text = stringResource(R.string.custom_theme_color_picker_cancel),
                onClick = onDismiss,
                style = FunputButtonStyle.SECONDARY,
                modifier = Modifier.weight(1f),
            )
            FunputButton(
                text = stringResource(R.string.custom_theme_color_picker_confirm),
                onClick = { onConfirm(state.argb) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ColorPreview(argb: Int) {
    val shape = FunputUi.shapes.iconTile
    // A transparent colour would otherwise be an empty square; the border marks where it is.
    Box(
        Modifier
            .size(48.dp)
            .clip(shape)
            .background(Color(argb))
            .border(1.dp, FunputUi.colors.separator, shape),
    )
}

@Composable
private fun HexField(state: ColorPickerState, modifier: Modifier, onChange: (ColorPickerState) -> Unit) {
    var text by remember(state.argb) { mutableStateOf(state.hex) }
    FunputTextField(
        value = text,
        onValueChange = { input ->
            text = input
            ColorPickerState.fromHexOrNull(input, state.alpha)?.let(onChange)
        },
        label = stringResource(R.string.custom_theme_color_picker_hex),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
        modifier = modifier.testTag(ColorPickerHexTag),
    )
}

/** Test tag of the hex field. */
internal const val ColorPickerHexTag = "theme-color-hex"

private const val MaxHue = 360f
private const val PercentScale = 100f
