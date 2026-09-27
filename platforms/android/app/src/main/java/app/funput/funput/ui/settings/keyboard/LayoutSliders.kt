package app.funput.funput.ui.settings.keyboard

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.keyboard.KeyboardDimensions
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.keyboard.placement.KeyboardPlacementMode
import app.funput.funput.keyboard.placement.KeyboardPlacementPreferences
import app.funput.funput.keyboard.placement.KeyboardPlacementResolver
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.SliderRow
import app.funput.funput.ui.kit.theme.FunputTint
import kotlin.math.roundToInt

private const val PercentScale = 100f

/**
 * A percentage setting. The slider moves freely while dragged and reports once, on release,
 * rounded to a whole percent: a keyboard three thousandths taller is not a size anybody asked for.
 */
@Composable
internal fun PercentSliderRow(
    title: String,
    @DrawableRes icon: Int,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onSettled: (Float) -> Unit,
    showsRange: Boolean = false,
) {
    var draft by remember(value) { mutableFloatStateOf(value) }
    SliderRow(
        title = title,
        icon = icon,
        tint = FunputTint.BLUE,
        value = draft,
        onValueChange = { draft = it },
        onValueChangeFinished = { onSettled((draft * PercentScale).roundToInt() / PercentScale) },
        valueRange = range,
        valueLabel = percentLabel(draft),
        rangeLabels = if (showsRange) percentLabel(range.start) to percentLabel(range.endInclusive) else null,
    )
}

/** How far the elevated keyboard is lifted, in whole dp, up to what the screen can fit. */
@Composable
internal fun ElevationSliderRow(valueDp: Float, maximumDp: Float, onSettled: (Float) -> Unit) {
    val safeMaximum = maximumDp.coerceAtLeast(1f)
    var draft by remember(valueDp, safeMaximum) { mutableFloatStateOf(valueDp.coerceIn(0f, safeMaximum)) }
    SliderRow(
        title = stringResource(R.string.settings_elevation_title),
        icon = FunputIcons.Placement,
        tint = FunputTint.BLUE,
        value = draft,
        onValueChange = { draft = it },
        onValueChangeFinished = { onSettled(draft.roundToInt().toFloat()) },
        valueRange = 0f..safeMaximum,
        valueLabel = stringResource(R.string.settings_elevation_value, draft.roundToInt()),
        enabled = maximumDp > 0f,
    )
}

/**
 * The most the keyboard can be raised on this screen: what the placement resolver allows for an
 * elevated keyboard of the current size, asked with an offset larger than any screen.
 */
@Composable
internal fun elevationMaximumDp(
    inputMethod: KeyboardInputMethod,
    keySizeProfile: KeyboardSizingProfile,
    showsNumberRow: Boolean,
    placement: KeyboardPlacementPreferences,
): Float {
    val configuration = LocalConfiguration.current
    val baseHeight = KeyboardDimensions.recommendedHeightDp(
        inputMethod, KeyboardEditorMode.TEXT, keySizeProfile,
        configuration.screenWidthDp.toFloat(), showsNumberRow,
    )
    return KeyboardPlacementResolver.resolve(
        preferences = placement.copy(
            activeMode = KeyboardPlacementMode.ELEVATED,
            elevatedOffsetDp = configuration.screenHeightDp.toFloat(),
        ),
        density = 1f,
        viewportHeightPx = configuration.screenHeightDp,
        baseKeyboardHeightPx = baseHeight.toInt(),
    ).maximumOffsetPx.toFloat()
}

@Composable
private fun percentLabel(value: Float): String =
    stringResource(R.string.settings_percent_value, (value * PercentScale).roundToInt())
