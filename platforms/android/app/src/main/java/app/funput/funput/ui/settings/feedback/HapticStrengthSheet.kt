package app.funput.funput.ui.settings.feedback

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.keyboard.KeyboardHapticStrength
import app.funput.funput.keyboard.KeyboardHaptics
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.overlays.FunputSheet
import app.funput.funput.ui.kit.rows.ChoiceRow

/**
 * Choosing how strongly the keyboard vibrates: off, light, medium or strong. Unlike other pickers
 * it stays open, and each strength plays once as it is chosen, so the user can feel them in turn.
 */
@Composable
internal fun HapticStrengthSheet(
    selected: KeyboardHapticStrength?,
    onSelect: (KeyboardHapticStrength?) -> Unit,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    FunputSheet(onDismiss = onDismiss, title = stringResource(R.string.settings_haptics_title)) {
        FunputSection(title = null, footer = stringResource(R.string.haptic_strength_hint)) {
            HapticStrengthOption.entries.forEachIndexed { index, option ->
                if (index > 0) FunputDivider()
                ChoiceRow(
                    title = stringResource(option.title),
                    summary = stringResource(option.summary),
                    selected = option.strength == selected,
                    onSelect = {
                        onSelect(option.strength)
                        option.strength?.let { strength -> KeyboardHaptics.performSample(view, strength) }
                    },
                    modifier = Modifier.testTag(hapticStrengthTag(option)),
                )
            }
        }
    }
}

/** Test tag of the row for [option]. */
internal fun hapticStrengthTag(option: HapticStrengthOption): String = "haptic-strength-${option.name.lowercase()}"
