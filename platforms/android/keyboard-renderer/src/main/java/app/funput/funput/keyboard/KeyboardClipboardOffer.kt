package app.funput.funput.keyboard

import android.content.res.Resources
import androidx.annotation.StringRes

/** Metadata-only description of clipboard content offered by the keyboard toolbar. */
enum class KeyboardClipboardHint(
    @param:StringRes internal val titleResource: Int,
    @param:StringRes internal val chipResource: Int,
) {
    TEXT(R.string.clipboard_hint_text, R.string.clipboard_chip_text),
    LINK(R.string.clipboard_hint_link, R.string.clipboard_chip_link),
    SENSITIVE(R.string.clipboard_hint_sensitive, R.string.clipboard_chip_sensitive),
}

/**
 * Clipboard content the toolbar offers to paste.
 *
 * @property preview One shortened line of the clip, when the host already holds its text. An
 * offer never reads the clipboard to get one, and a sensitive clip is never previewed.
 * @property showsChip False once the user has started typing: the paste chip leaves the
 * suggestion band, while the badge on the clipboard key stays.
 */
data class KeyboardClipboardOffer(
    val hint: KeyboardClipboardHint,
    val preview: String? = null,
    val showsChip: Boolean = true,
) {
    /** [preview], withheld for a sensitive clip whatever the host passed. */
    internal val visiblePreview: String?
        get() = preview?.takeIf { hint != KeyboardClipboardHint.SENSITIVE && it.isNotBlank() }
}

internal fun KeyboardClipboardHint.title(resources: Resources): String =
    resources.getString(titleResource)

internal fun KeyboardClipboardHint.accessibilityLabel(resources: Resources): String =
    resources.getString(R.string.clipboard_paste_accessibility, title(resources))

/** What the paste chip shows: the clip itself when it may, else what kind of clip it is. */
internal fun KeyboardClipboardOffer.chipLabel(resources: Resources): String =
    visiblePreview ?: resources.getString(hint.chipResource)

internal fun KeyboardClipboardOffer.chipAccessibilityLabel(resources: Resources): String =
    resources.getString(R.string.clipboard_paste_accessibility, visiblePreview ?: hint.title(resources))
