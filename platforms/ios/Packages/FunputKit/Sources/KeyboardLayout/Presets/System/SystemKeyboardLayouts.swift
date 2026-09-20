/// The letter pages of the system preset.
///
/// Rows one through three hold the same keys as the Funput preset, but the Shift row and the
/// action row are placed on the letter grid the way Apple places them, and the action row
/// drops the comma and period keys.
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
    /// Including the number row: it answers the preference exactly as the letters page
    /// does, VNI aside. Search used to keep the row whatever the user asked, on the
    /// grounds that the Funput preset's search page always carries digits — it does not,
    /// it goes compact there like every other QWERTY page.
    public static func search(
        _ inputMethod: KeyboardInputMethod,
        showsNumberRow: Bool = true
    ) -> KeyboardLayout {
        page(
            id: "qwerty-search-\(inputMethod.rawValue)-system",
            page: "system-search",
            inputMethod: inputMethod,
            hasNumberRow: inputMethod == .vni || showsNumberRow
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
            leadingRows: hasNumberRow
                ? [topNumberRow(for: inputMethod, pageID: "\(page)-\(inputMethod.rawValue)")]
                : [],
            actionKeys: systemLettersActionRow(page: page).keys,
            actionSpans: SystemRowSpans.action,
            bottomRowSpans: SystemRowSpans.bottomLetters(hasNumberRow: hasNumberRow),
            showsTelexHints: inputMethod.isTelexFamily,
            supportsVietnameseAlternates: true
        )
    }
}
