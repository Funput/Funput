package app.funput.funput.ime.hardware.tile

import android.service.quicksettings.Tile
import androidx.annotation.StringRes
import app.funput.funput.ime.R

/**
 * Pure presentation model for the Control Center / Quick Settings tile that toggles whether Funput
 * stays visible while a physical keyboard is attached.
 */
internal data class HardwareKeyboardTileState(
    val showsSoftKeyboard: Boolean,
    val tileState: Int,
    @param:StringRes val labelRes: Int,
    @param:StringRes val subtitleRes: Int,
    @param:StringRes val contentDescriptionRes: Int,
) {
    /** Flips the setting immediately for an optimistic tile update on tap. */
    fun toggled(): HardwareKeyboardTileState = from(showsSoftKeyboard = !showsSoftKeyboard)

    companion object {
        fun from(showsSoftKeyboard: Boolean): HardwareKeyboardTileState =
            if (showsSoftKeyboard) {
                HardwareKeyboardTileState(
                    showsSoftKeyboard = true,
                    tileState = Tile.STATE_ACTIVE,
                    labelRes = R.string.hardware_keyboard_tile_label,
                    subtitleRes = R.string.hardware_keyboard_tile_subtitle_shown,
                    contentDescriptionRes = R.string.hardware_keyboard_tile_description_shown,
                )
            } else {
                HardwareKeyboardTileState(
                    showsSoftKeyboard = false,
                    tileState = Tile.STATE_INACTIVE,
                    labelRes = R.string.hardware_keyboard_tile_label,
                    subtitleRes = R.string.hardware_keyboard_tile_subtitle_hidden,
                    contentDescriptionRes = R.string.hardware_keyboard_tile_description_hidden,
                )
            }
    }
}
