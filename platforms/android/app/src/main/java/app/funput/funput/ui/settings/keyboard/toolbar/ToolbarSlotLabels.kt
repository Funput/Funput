package app.funput.funput.ui.settings.keyboard.toolbar

import androidx.annotation.StringRes
import app.funput.funput.R
import app.funput.funput.keyboard.layout.toolbar.ToolbarSlot
import app.funput.funput.ui.kit.icons.FunputIcons

/** Titles and icons for each toolbar slot in the layout settings list. */
internal object ToolbarSlotLabels {
    @StringRes
    fun title(slot: ToolbarSlot): Int = when (slot) {
        ToolbarSlot.MICROPHONE -> R.string.settings_toolbar_slot_microphone
        ToolbarSlot.PLACEMENT -> R.string.settings_keyboard_mode_title
        ToolbarSlot.CLIPBOARD -> R.string.settings_toolbar_slot_clipboard
        ToolbarSlot.EMOJI -> R.string.settings_toolbar_slot_emoji
    }

    fun icon(slot: ToolbarSlot): Int = when (slot) {
        ToolbarSlot.MICROPHONE -> FunputIcons.Microphone
        ToolbarSlot.PLACEMENT -> FunputIcons.Placement
        ToolbarSlot.CLIPBOARD -> FunputIcons.Clipboard
        ToolbarSlot.EMOJI -> FunputIcons.Suggestions
    }
}
