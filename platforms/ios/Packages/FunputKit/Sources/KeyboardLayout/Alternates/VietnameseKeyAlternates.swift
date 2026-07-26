public enum VietnameseKeyAlternates {
    public static func values(for character: Character) -> [KeyAlternate] {
        guard let values = catalog[character.lowercased().first ?? character] else { return [] }
        return values.map { KeyAlternate(text: $0) }
    }

    private static let catalog: [Character: [String]] = [
        "a": groups("aáàảãạ", "ăắằẳẵặ", "âấầẩẫậ"),
        "e": groups("eéèẻẽẹ", "êếềểễệ"),
        "i": groups("iíìỉĩị"),
        "o": groups("oóòỏõọ", "ôốồổỗộ", "ơớờởỡợ"),
        "u": groups("uúùủũụ", "ưứừửữự"),
        "y": groups("yýỳỷỹỵ"),
        "d": groups("dđ"),
    ]

    private static func groups(_ values: String...) -> [String] {
        values.flatMap { $0.map(String.init) }
    }
}
