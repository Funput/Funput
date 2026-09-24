enum SystemSymbolPageContent {
    // The seventh glyph is "₫" (U+20AB), the dong sign, where the US keyboard has "$" —
    // not "đ" (U+0111), the letter. Apple's Vietnamese keyboard draws it with the bar
    // underneath, and the two are trivially confusable in a diff.
    static let primaryRow = ["-", "/", ":", ";", "(", ")", "\u{20AB}", "&", "@", "\""]

    static let secondaryRowUpper = ["[", "]", "{", "}", "#", "%", "^", "*", "+", "="]
    static let secondaryRowLower = ["_", "\\", "|", "~", "<", ">", "$", "¥", "€", "•"]

    /// Both symbol pages repeat this row between the symbols and the action row.
    static let punctuationRow = [".", ",", "?", "!", "'"]
}
