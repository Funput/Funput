/// Single source of truth for "this page drops its top number row".
///
/// Only pages that can put the digits back on the letters row honour the preference: the
/// QWERTY pages — text, search, email and URL — hand them to `CompactDigitAlternates`. The
/// keypads and the secure pages have no such row to give them, and VNI spends the row on
/// tone modifiers, so neither ever goes compact. Layout, symbol page and measured height
/// must all agree on this answer, otherwise a five-row keyboard gets squeezed into a
/// four-row panel.
func usesCompactLetterRows(
    inputMethod: KeyboardInputMethod,
    editorMode: KeyboardEditorMode,
    showsNumberRow: Bool
) -> Bool {
    editorMode.supportsCompactLetterRows && inputMethod.isTelexFamily && !showsNumberRow
}
