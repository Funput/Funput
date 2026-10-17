package app.funput.funput.ui.kit.icons

import androidx.annotation.DrawableRes
import app.funput.funput.ui.kit.R

/**
 * The tab bar's icons: outlined while a tab is unselected and filled once it is, so the selection
 * reads from the glyph itself as well as from the capsule behind it.
 */
object FunputTabIcons {
    @DrawableRes val Keyboard: Int = R.drawable.ph_keyboard
    @DrawableRes val KeyboardSelected: Int = R.drawable.ph_keyboard_fill

    @DrawableRes val Shortcuts: Int = R.drawable.ph_lightning
    @DrawableRes val ShortcutsSelected: Int = R.drawable.ph_lightning_fill

    @DrawableRes val Appearance: Int = R.drawable.ph_palette
    @DrawableRes val AppearanceSelected: Int = R.drawable.ph_palette_fill

    @DrawableRes val News: Int = R.drawable.ph_newspaper
    @DrawableRes val NewsSelected: Int = R.drawable.ph_newspaper_fill

    @DrawableRes val Me: Int = R.drawable.ph_user_circle
    @DrawableRes val MeSelected: Int = R.drawable.ph_user_circle_fill
}
