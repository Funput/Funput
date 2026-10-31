package app.funput.funput.ui.kit.icons

import androidx.annotation.DrawableRes
import app.funput.funput.ui.kit.R

/**
 * Icons for a list the user keeps, such as the personal dictionary: ordering it, acting on one
 * of its items, and saying that it stays on the device. Named by meaning, like [FunputIcons].
 */
object FunputListIcons {
    /** Words the user keeps, such as the personal dictionary. */
    @DrawableRes val Dictionary: Int = R.drawable.ph_book_open

    /** Choosing the order a list is shown in. */
    @DrawableRes val Sort: Int = R.drawable.ph_arrows_down_up

    /** Copying an item's text. */
    @DrawableRes val Copy: Int = R.drawable.ph_copy

    /** Keeping an item out for good, such as a word never to suggest. */
    @DrawableRes val Block: Int = R.drawable.ph_prohibit

    /** Kept on this device and never sent anywhere. */
    @DrawableRes val Private: Int = R.drawable.ph_lock_simple
}
