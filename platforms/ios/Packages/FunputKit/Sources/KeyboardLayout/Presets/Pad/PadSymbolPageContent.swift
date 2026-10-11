/// The glyphs of the stock iPad symbol pages, between the keys at either end.
enum PadSymbolPageContent {
    // "₫" is the dong sign (U+20AB) where the US keyboard has "$" — not "đ" (U+0111).
    static let primaryRows = [
        ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"],
        ["@", "#", "\u{20AB}", "&", "*", "(", ")", "'", "\""],
        ["%", "-", "+", "=", "/", ";", ":", ",", "."],
    ]

    static let secondaryRows = [
        ["[", "]", "{", "}", "#", "%", "^", "*", "+", "="],
        ["_", "\\", "|", "~", "<", ">", "$", "€", "¥"],
        ["•", "…", "§", "!", "?", "¿", "¡", ",", "."],
    ]
}
