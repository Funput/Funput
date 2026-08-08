enum SystemSymbolPageContent {
    // The seventh glyph is "đ" (U+0111), the Vietnamese LETTER — not "₫" (U+20AB), the
    // currency sign that `SymbolPageContent.primaryRow1` carries. Apple's Vietnamese
    // keyboard puts the letter here, and the two are trivially confusable in a diff.
    static let primaryRow = ["-", "/", ":", ";", "(", ")", "đ", "&", "@", "\""]

    static let secondaryRowUpper = ["[", "]", "{", "}", "#", "%", "^", "*", "+", "="]
    static let secondaryRowLower = ["_", "\\", "|", "~", "<", ">", "$", "¥", "€", "•"]

    /// Both symbol pages repeat this row between the symbols and the action row.
    static let punctuationRow = [".", ",", "?", "!", "'"]
}
