package app.funput.funput.ime.editing.capitalization

import android.text.TextUtils
import app.funput.funput.ime.editing.EditorInfoPolicy

/** How much of what is typed next the keyboard should upper-case. */
internal enum class AutoCapitalizationMode {
    NONE,
    WORDS,
    SENTENCES,
    ALL_CHARACTERS,
}

/**
 * Resolves what the editor asks for against what the user asked for.
 *
 * Reading the four branches in order:
 *
 * 1. A field that is never prose — a password, an address, a number pad — is left
 *    alone whatever anyone asked for.
 * 2. A field demanding upper case wins even against a disabled preference: that is
 *    a statement about what the field holds, not a convenience being offered.
 * 3. The user's "Tự viết hoa" switch silences the two convenience modes.
 * 4. Otherwise sentences, **including when the editor set no CAP flag at all**.
 *    Android has no default here and most apps never set one, so waiting to be
 *    asked left the feature off nearly everywhere; iOS gets `.sentences` by default
 *    from UIKit and that is the behaviour being matched.
 */
internal fun EditorInfoPolicy.autoCapitalizationMode(
    preferenceEnabled: Boolean,
): AutoCapitalizationMode = when {
    !allowsAutoCapitalization -> AutoCapitalizationMode.NONE
    capitalizationModes has TextUtils.CAP_MODE_CHARACTERS -> AutoCapitalizationMode.ALL_CHARACTERS
    !preferenceEnabled -> AutoCapitalizationMode.NONE
    capitalizationModes has TextUtils.CAP_MODE_WORDS -> AutoCapitalizationMode.WORDS
    else -> AutoCapitalizationMode.SENTENCES
}

private infix fun Int.has(flag: Int): Boolean = this and flag != 0
