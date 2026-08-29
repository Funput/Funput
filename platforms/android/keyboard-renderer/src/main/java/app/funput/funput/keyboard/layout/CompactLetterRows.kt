package app.funput.funput.keyboard.layout

import app.funput.funput.keyboard.model.KeyboardEditorMode
import app.funput.funput.keyboard.model.KeyboardInputMethod

/**
 * Single source of truth for "this keyboard drops its top number row".
 *
 * Only the pages that can hand the digits to [CompactDigitAlternates] honour the preference: the
 * QWERTY pages — text, search, email and URL. The keypads and the secure pages have no such row to
 * give them, and VNI needs the digits for its tone marks. Layout and measured height must agree on
 * this, otherwise a five-row keyboard gets squeezed into a four-row panel.
 */
internal fun usesCompactLetterRows(
    inputMethod: KeyboardInputMethod,
    editorMode: KeyboardEditorMode,
    showsNumberRow: Boolean,
): Boolean = editorMode in CompactCapableEditorModes &&
    inputMethod.isTelexFamily &&
    !showsNumberRow

private val CompactCapableEditorModes = setOf(
    KeyboardEditorMode.TEXT,
    KeyboardEditorMode.SEARCH,
    KeyboardEditorMode.EMAIL,
    KeyboardEditorMode.URL,
)
