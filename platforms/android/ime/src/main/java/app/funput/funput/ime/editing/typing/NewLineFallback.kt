package app.funput.funput.ime.editing.typing

import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.keyboard.model.KeyAction

/**
 * A forced line break only belongs in a multi-line field. A single-line field would keep the
 * `\n` it is handed (it only draws it as a space), so there Shift+Enter is an ordinary Enter.
 */
internal fun KeyAction.forEditor(policy: EditorInfoPolicy): KeyAction =
    if (this == KeyAction.NewLine && !policy.isMultiline) KeyAction.Enter else this
