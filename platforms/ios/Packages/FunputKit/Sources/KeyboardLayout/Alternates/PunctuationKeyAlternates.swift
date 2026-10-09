/// Symbols offered by holding a punctuation key, so the everyday ones are one gesture away
/// from the letters page. The period catalog and its order match Android's.
public enum PunctuationKeyAlternates {
    /// Two rows of eight: wide enough to read as a strip above the action row, short
    /// enough to stay inside the keyboard on the smallest phones.
    public static let periodColumns = 8

    public static let period: [KeyAlternate] = [
        symbol("@", "a còng"),
        symbol("&", "dấu và"),
        symbol("%", "phần trăm"),
        symbol("+", "dấu cộng"),
        symbol(";", "dấu chấm phẩy"),
        symbol("/", "dấu gạch chéo"),
        symbol("(", "ngoặc mở"),
        symbol(")", "ngoặc đóng"),
        symbol("\"", "dấu nháy kép"),
        symbol("'", "dấu nháy đơn"),
        symbol("#", "dấu thăng"),
        symbol("-", "dấu trừ"),
        symbol(":", "dấu hai chấm"),
        symbol("!", "dấu chấm than"),
        symbol("?", "dấu hỏi"),
    ]

    /// Symbols have no case, so Shift must not change what the cell types or announces.
    private static func symbol(_ text: String, _ accessibilityLabel: String) -> KeyAlternate {
        KeyAlternate(text: text, shiftedText: text, accessibilityLabel: accessibilityLabel)
    }
}
