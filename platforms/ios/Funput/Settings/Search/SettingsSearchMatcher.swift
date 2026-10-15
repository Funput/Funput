import Foundation

/// Finds settings by what people type, with or without Vietnamese accents.
enum SettingsSearchMatcher {
    /// Entries where every word of `query` appears in the title or a keyword.
    static func matches(
        _ query: String,
        in entries: [SettingsSearchEntry] = SettingsSearchEntry.all
    ) -> [SettingsSearchEntry] {
        let words = normalize(query).split(whereSeparator: \.isWhitespace).map(String.init)
        guard !words.isEmpty else { return [] }
        return entries.filter { entry in
            let haystack = normalize(([entry.title] + entry.keywords).joined(separator: " "))
            return words.allSatisfy(haystack.contains)
        }
    }

    /// Lowercased and accent-free. "đ" is not a diacritic to Foundation — it is its own
    /// letter — so it is mapped by hand, or "dat dau" would never find "đặt dấu".
    static func normalize(_ text: String) -> String {
        text.replacingOccurrences(of: "đ", with: "d")
            .replacingOccurrences(of: "Đ", with: "D")
            .folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "vi"))
    }
}
