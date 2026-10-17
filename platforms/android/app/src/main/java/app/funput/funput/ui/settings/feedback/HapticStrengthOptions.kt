package app.funput.funput.ui.settings.feedback

import androidx.annotation.StringRes
import app.funput.funput.R
import app.funput.funput.keyboard.KeyboardHapticStrength

/** One row of the strength sheet: off, or a strength, with its name and what it feels like. */
internal enum class HapticStrengthOption(
    val strength: KeyboardHapticStrength?,
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int,
) {
    OFF(null, R.string.haptic_strength_off, R.string.haptic_strength_off_summary),
    LIGHT(KeyboardHapticStrength.LIGHT, R.string.haptic_strength_light, R.string.haptic_strength_light_summary),
    MEDIUM(KeyboardHapticStrength.MEDIUM, R.string.haptic_strength_medium, R.string.haptic_strength_medium_summary),
    STRONG(KeyboardHapticStrength.STRONG, R.string.haptic_strength_strong, R.string.haptic_strength_strong_summary),
    ;

    companion object {
        /** The option for [strength], where null is off. */
        fun of(strength: KeyboardHapticStrength?): HapticStrengthOption =
            entries.first { option -> option.strength == strength }
    }
}
