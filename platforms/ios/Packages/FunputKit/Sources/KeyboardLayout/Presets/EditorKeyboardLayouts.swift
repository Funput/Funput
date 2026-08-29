public enum EditorKeyboardLayouts {
    public static func resolve(
        _ inputMethod: KeyboardInputMethod,
        editorMode: KeyboardEditorMode,
        showsNumberRow: Bool = true
    ) -> KeyboardLayout {
        switch editorMode {
        case .text:
            StandardKeyboardLayouts.letters(inputMethod, showsNumberRow: showsNumberRow)
        case .search, .email, .url:
            webLayout(editorMode, inputMethod: inputMethod, showsNumberRow: showsNumberRow)
        case .phone:
            PhoneKeyboardLayouts.resolve(inputMethod)
        case .password:
            PasswordKeyboardLayouts.text(inputMethod)
        case .pin:
            PasswordKeyboardLayouts.pin(inputMethod)
        case .number, .numberDecimal, .numberSigned, .numberSignedDecimal:
            NumberKeyboardLayouts.resolve(inputMethod, mode: editorMode)
        }
    }

    /// The QWERTY page behind search, email and URL: one shape, one action row, and one
    /// answer to the number row preference.
    ///
    /// Search composes Vietnamese, so it carries the same tone hints and the same VNI
    /// modifier row as the letters page — a field where the tones work but their hints are
    /// hidden, and where Shift is eaten by a tone key, just behaves differently for no
    /// reason the user can see. Email and URL do not compose: no tone hints, plain digits.
    ///
    /// All three follow the letters page on the number row: a Telex typist who turned it
    /// off gets the four-row page here too, with the digits one long press away on the top
    /// row. VNI keeps the row, which is where its tone modifiers live.
    private static func webLayout(
        _ editorMode: KeyboardEditorMode,
        inputMethod: KeyboardInputMethod,
        showsNumberRow: Bool
    ) -> KeyboardLayout {
        let composes = editorMode.supportsVietnameseComposition
        let pageID = "\(editorMode.rawValue)-\(inputMethod.rawValue)"
        let isCompact = usesCompactLetterRows(
            inputMethod: inputMethod,
            editorMode: editorMode,
            showsNumberRow: showsNumberRow
        )
        let numberRow = composes
            ? topNumberRow(for: inputMethod, pageID: pageID)
            : topNumberRow(.plainCharacter, pageID: pageID)
        let layout = qwertyLayout(
            id: "qwerty-\(pageID)\(isCompact ? "-compact" : "")",
            inputMethod: inputMethod,
            leadingRows: isCompact ? [] : [numberRow],
            actionKeys: webActionKeys(editorMode),
            showsTelexHints: composes,
            supportsVietnameseAlternates: composes
        )
        return isCompact ? CompactDigitAlternates.decorate(layout) : layout
    }

    /// Email reaches for `@`; search and URL reach for `/`. Only search keeps the language
    /// swipe on the space bar, because only search composes.
    private static func webActionKeys(_ editorMode: KeyboardEditorMode) -> [KeySpec] {
        let middle = editorMode == .email
            ? (id: "at", label: "@", accessibility: "A còng")
            : (id: "slash", label: "/", accessibility: "Dấu gạch chéo")
        return [
            specialKey("symbols", "?123", .symbols, weight: 1.7, accessibilityLabel: "Ký hiệu"),
            specialKey(
                middle.id,
                middle.label,
                .punctuation,
                accessibilityLabel: middle.accessibility
            ),
            editorMode.supportsVietnameseComposition
                ? standardSpaceKey()
                : asciiSpaceKey(weight: 5.8),
            specialKey("period", ".", .punctuation, accessibilityLabel: "Dấu chấm"),
            specialKey("enter", "", .enter, weight: 1.7, accessibilityLabel: "Enter"),
        ]
    }
}
