/// The letters page of the system preset.
///
/// Rows one through three are the same QWERTY block the Funput preset uses — only the
/// action row differs, dropping the comma and period keys and gaining an emoji key
/// where Apple puts one.
public enum SystemKeyboardLayouts {
    public static func letters(
        _ inputMethod: KeyboardInputMethod,
        showsNumberRow: Bool = true
    ) -> KeyboardLayout {
        // VNI composes tones with the digits, so it always keeps the number row. That
        // matches the stock keyboard, which also shows digits for Vietnamese VNI.
        let hasNumberRow = inputMethod == .vni || showsNumberRow
        return qwertyLayout(
            id: "qwerty-\(inputMethod.rawValue)-system\(hasNumberRow ? "" : "-compact")",
            inputMethod: inputMethod,
            leadingRows: hasNumberRow ? [topNumberRowForLetters(inputMethod)] : [],
            actionKeys: systemLettersActionRow(page: "system-letters").keys,
            showsTelexHints: inputMethod.isTelexFamily,
            supportsVietnameseAlternates: true
        )
    }
}
