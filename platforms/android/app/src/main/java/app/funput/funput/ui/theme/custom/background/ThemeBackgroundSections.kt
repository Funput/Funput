package app.funput.funput.ui.theme.custom.background

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.ui.kit.cards.FunputCard
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.rows.ActionRow
import app.funput.funput.ui.kit.rows.SliderRow
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.theme.custom.draft.ThemeDraftState
import app.funput.funput.ui.theme.custom.color.ColorRow
import app.funput.funput.ui.theme.custom.color.picker.ColorPickerSheet
import java.util.Locale

/**
 * The background image's controls: with none yet, a way to choose one; with one, which part shows,
 * how it is blended, its colours as accents, and changing or removing it.
 */
@Composable
internal fun ThemeBackgroundSections(state: ThemeDraftState, onChooseImage: () -> Unit) {
    val image = state.backgroundImage
    if (image == null) {
        FunputCard(contentPadding = PaddingValues(FunputUi.spacing.cardPadding)) {
            Column(verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.medium)) {
                BasicText(
                    text = stringResource(R.string.custom_theme_background_empty),
                    style = FunputUi.typography.body.copy(color = FunputUi.colors.secondaryLabel),
                )
                FunputButton(
                    text = stringResource(R.string.custom_theme_background_choose),
                    onClick = onChooseImage,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        return
    }
    val bitmap by rememberBackgroundBitmap(image.source)
    val palette by rememberImagePalette(bitmap)
    FunputSection(
        title = stringResource(R.string.custom_theme_background_frame),
        footer = stringResource(R.string.custom_theme_background_focus_hint),
    ) {
        BackgroundFocusPicker(
            source = image.source,
            focalX = image.focalX,
            focalY = image.focalY,
            onFocusChange = { x, y -> state.updateBackgroundImage { it.copy(focalX = x, focalY = y) } },
            modifier = Modifier.padding(FunputUi.spacing.cardPadding),
        )
    }
    BlendSection(state, image)
    ImagePaletteSection(colours = palette, onSelected = state::applyAccent)
    FunputSection(title = null) {
        ActionRow(title = stringResource(R.string.custom_theme_background_change), onClick = onChooseImage)
        FunputDivider()
        ActionRow(
            title = stringResource(R.string.custom_theme_background_remove),
            onClick = { state.backgroundImage = null },
            destructive = true,
        )
    }
}

/** How the image sits under the keys: how much shows, how close, how soft, and what tints it. */
@Composable
private fun BlendSection(state: ThemeDraftState, image: KeyboardThemeBackgroundImage) {
    var editingOverlay by remember { mutableStateOf(false) }
    FunputSection(title = stringResource(R.string.custom_theme_background_adjust)) {
        BackgroundSlider(R.string.custom_theme_background_opacity_label, image.opacity, 0f..1f, percent(image.opacity)) {
            state.updateBackgroundImage { image -> image.copy(opacity = it) }
        }
        FunputDivider()
        val zoom = String.format(Locale.ROOT, "%.1f×", image.zoom)
        BackgroundSlider(R.string.custom_theme_background_zoom_label, image.zoom, KeyboardThemeBackgroundImage.ZoomRange, zoom) {
            state.updateBackgroundImage { image -> image.copy(zoom = it) }
        }
        FunputDivider()
        val blur = String.format(Locale.ROOT, "%.0f dp", image.blurRadiusDp)
        BackgroundSlider(R.string.custom_theme_background_blur_label, image.blurRadiusDp, KeyboardThemeBackgroundImage.BlurRange, blur) {
            state.updateBackgroundImage { image -> image.copy(blurRadiusDp = it) }
        }
        FunputDivider()
        ColorRow(
            label = stringResource(R.string.custom_theme_background_overlay),
            color = image.overlayColor,
            onClick = { editingOverlay = true },
        )
    }
    if (editingOverlay) {
        ColorPickerSheet(
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

@Composable
private fun BackgroundSlider(
    titleRes: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onChange: (Float) -> Unit,
) {
    SliderRow(
        title = stringResource(titleRes),
        value = value,
        onValueChange = onChange,
        valueRange = range,
        valueLabel = valueLabel,
    )
}

private fun percent(value: Float): String = "${(value * 100f).toInt()}%"
