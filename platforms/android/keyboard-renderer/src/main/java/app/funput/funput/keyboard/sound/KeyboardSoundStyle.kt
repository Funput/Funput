package app.funput.funput.keyboard.sound

import androidx.annotation.RawRes
import app.funput.funput.keyboard.R

/**
 * What a key press sounds like: the system keyboard click, or one of the sounds Funput bundles.
 * The on/off switch is separate, so turning sound off and on again keeps the style.
 */
enum class KeyboardSoundStyle(val storageId: String) {
    SYSTEM("system"),
    MECHANICAL("mechanical"),
    WOOD("wood"),
    BUBBLE("bubble"),
    TYPEWRITER("typewriter"),
    MARIMBA("marimba"),
    ;

    /** The bundled file for [kind]; the system style has none. */
    @RawRes
    internal fun rawResource(kind: KeyboardSoundKind): Int? = when (this) {
        SYSTEM -> null
        MECHANICAL -> kind.pick(
            R.raw.key_sound_mechanical_key, R.raw.key_sound_mechanical_delete, R.raw.key_sound_mechanical_modifier,
        )
        WOOD -> kind.pick(R.raw.key_sound_wood_key, R.raw.key_sound_wood_delete, R.raw.key_sound_wood_modifier)
        BUBBLE -> kind.pick(R.raw.key_sound_bubble_key, R.raw.key_sound_bubble_delete, R.raw.key_sound_bubble_modifier)
        TYPEWRITER -> kind.pick(
            R.raw.key_sound_typewriter_key, R.raw.key_sound_typewriter_delete, R.raw.key_sound_typewriter_modifier,
        )
        MARIMBA -> kind.pick(
            R.raw.key_sound_marimba_key, R.raw.key_sound_marimba_delete, R.raw.key_sound_marimba_modifier,
        )
    }

    companion object {
        /** The system click Funput played before styles existed, so nobody's keyboard sounds different on update. */
        val Default = SYSTEM

        /** The style stored as [value], or [Default] for a missing or unknown one. */
        fun fromStorage(value: String?): KeyboardSoundStyle =
            entries.firstOrNull { style -> style.storageId == value } ?: Default
    }
}

private fun KeyboardSoundKind.pick(key: Int, delete: Int, modifier: Int): Int = when (this) {
    KeyboardSoundKind.KEY -> key
    KeyboardSoundKind.DELETE -> delete
    KeyboardSoundKind.MODIFIER -> modifier
}
