package app.funput.funput.keyboard

import android.content.res.Resources
import androidx.annotation.StringRes

/** Metadata-only description of clipboard content offered by the keyboard toolbar. */
enum class KeyboardClipboardHint(@param:StringRes internal val titleResource: Int) {
    TEXT(R.string.clipboard_hint_text),
    LINK(R.string.clipboard_hint_link),
    SENSITIVE(R.string.clipboard_hint_sensitive),
}

internal fun KeyboardClipboardHint.title(resources: Resources): String =
    resources.getString(titleResource)

internal fun KeyboardClipboardHint.accessibilityLabel(resources: Resources): String =
    resources.getString(R.string.clipboard_paste_accessibility, title(resources))
