package app.funput.funput.ui.settings.summary

import android.content.res.Resources
import app.funput.funput.R

/**
 * Joins [items] the way a Vietnamese sentence does: "X", "X và Y", "X, Y và Z". Never with a
 * middle dot or a dash, which read as machine output.
 */
internal fun Resources.joinAsSentence(items: List<String>): String = when (items.size) {
    0 -> ""
    1 -> items.single()
    else -> getString(
        R.string.settings_summary_pair,
        items.dropLast(1).joinToString(getString(R.string.settings_summary_list_separator)),
        items.last(),
    )
}

/** "Đang tắt tất cả" when nothing in [enabled] is on, otherwise "Đang bật …" naming what is. */
internal fun Resources.enabledSentence(enabled: List<String>): String =
    if (enabled.isEmpty()) {
        getString(R.string.settings_summary_all_off)
    } else {
        getString(R.string.settings_summary_enabled, joinAsSentence(enabled))
    }

/** "Đang tắt tất cả", "Đang bật tất cả", or how many of [total] features are on. */
internal fun Resources.countSentence(on: Int, total: Int): String = when (on) {
    0 -> getString(R.string.settings_summary_all_off)
    total -> getString(R.string.settings_summary_all_on)
    else -> getString(R.string.settings_summary_some_on, on, total)
}
