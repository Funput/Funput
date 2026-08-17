package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod

/**
 * Single source of truth for "this keyboard drops its top number row".
 *
 * Only the plain-text letters layout honours the preference: the web/mail editors build their own
 * leading number row unconditionally, and VNI needs the digits for its tone marks. Layout and
 * measured height must agree on this, otherwise a five-row keyboard gets squeezed into a four-row
 * panel.
 */
internal fun usesCompactLetterRows(
    inputMethod: KeyboardInputMethod,
    editorMode: KeyboardEditorMode,
    showsNumberRow: Boolean,
): Boolean = editorMode == KeyboardEditorMode.TEXT &&
    inputMethod.isTelexFamily &&
    !showsNumberRow
