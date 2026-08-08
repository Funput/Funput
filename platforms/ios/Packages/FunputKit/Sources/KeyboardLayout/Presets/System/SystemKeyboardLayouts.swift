/// The letter pages of the system preset.
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
        return page(
            id: "qwerty-\(inputMethod.rawValue)-system\(hasNumberRow ? "" : "-compact")",
            page: "system-letters",
            inputMethod: inputMethod,
            hasNumberRow: hasNumberRow
        )
    }

    /// The search keyboard, which the stock keyboard renders exactly like the letters
    /// page — the magnifying glass on the return key comes from `enterAction`, not from
    /// the layout, so nothing here has to know about it.
    ///
    /// The number row stays whatever the preference says, as it does on the Funput
    /// preset's search layout: a search field is where digits are most likely to be
    /// wanted, and hiding them would take away something Telex users have today.
    public static func search(_ inputMethod: KeyboardInputMethod) -> KeyboardLayout {
        page(
            id: "qwerty-search-\(inputMethod.rawValue)-system",
            page: "system-search",
            inputMethod: inputMethod,
            hasNumberRow: true
        )
    }

    private static func page(
        id: String,
        page: String,
        inputMethod: KeyboardInputMethod,
        hasNumberRow: Bool
    ) -> KeyboardLayout {
        qwertyLayout(
            id: id,
            inputMethod: inputMethod,
            leadingRows: hasNumberRow ? [topNumberRowForLetters(inputMethod)] : [],
            actionKeys: systemLettersActionRow(page: page).keys,
            showsTelexHints: inputMethod.isTelexFamily,
            supportsVietnameseAlternates: true
        )
    }
}
