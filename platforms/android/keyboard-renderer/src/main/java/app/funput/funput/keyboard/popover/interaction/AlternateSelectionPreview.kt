package app.funput.funput.keyboard.popover.interaction

import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.popover.rendering.AlternatePaletteLayout

internal data class AlternateSelectionPreview(
    val key: KeySpec,
    val layout: AlternatePaletteLayout,
    val selectedIndex: Int?,
)
