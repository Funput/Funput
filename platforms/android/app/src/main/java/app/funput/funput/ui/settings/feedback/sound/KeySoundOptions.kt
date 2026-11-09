package app.funput.funput.ui.settings.feedback.sound

import androidx.annotation.StringRes
import app.funput.funput.R
import app.funput.funput.keyboard.sound.KeyboardSoundStyle
import app.funput.funput.keyboard.sound.KeyboardSoundVolume

/** One row of the key sound sheet: off, or a style, with its name and what it sounds like. */
internal enum class KeySoundOption(
    val style: KeyboardSoundStyle?,
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int,
) {
    OFF(null, R.string.key_sound_off, R.string.key_sound_off_summary),
    SYSTEM(KeyboardSoundStyle.SYSTEM, R.string.key_sound_system, R.string.key_sound_system_summary),
    MECHANICAL(KeyboardSoundStyle.MECHANICAL, R.string.key_sound_mechanical, R.string.key_sound_mechanical_summary),
    WOOD(KeyboardSoundStyle.WOOD, R.string.key_sound_wood, R.string.key_sound_wood_summary),
    BUBBLE(KeyboardSoundStyle.BUBBLE, R.string.key_sound_bubble, R.string.key_sound_bubble_summary),
    TYPEWRITER(KeyboardSoundStyle.TYPEWRITER, R.string.key_sound_typewriter, R.string.key_sound_typewriter_summary),
    MARIMBA(KeyboardSoundStyle.MARIMBA, R.string.key_sound_marimba, R.string.key_sound_marimba_summary),
    ;

    companion object {
        /** The option for [style], where null is off. */
        fun of(style: KeyboardSoundStyle?): KeySoundOption = entries.first { option -> option.style == style }
    }
}

/** The name of [volume] in the sheet's volume control. */
@StringRes
internal fun volumeTitle(volume: KeyboardSoundVolume): Int = when (volume) {
    KeyboardSoundVolume.SOFT -> R.string.key_sound_volume_soft
    KeyboardSoundVolume.MEDIUM -> R.string.key_sound_volume_medium
    KeyboardSoundVolume.LOUD -> R.string.key_sound_volume_loud
}
