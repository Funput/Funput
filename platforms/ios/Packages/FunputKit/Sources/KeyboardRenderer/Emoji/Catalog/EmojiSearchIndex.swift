import Foundation

struct EmojiSearchIndex {
    private struct Entry {
        let item: EmojiItem
        let names: [String]
        let terms: [String]
        let words: [String]
    }

    private let entries: [Entry]

    init(catalog: EmojiCatalog) {
        entries = catalog.emojis.map { item in
            let names = [item.localizedName, item.name]
                .compactMap { $0 }
                .map(Self.normalize)
            let terms = (names + item.searchTerms.map(Self.normalize))
                .filter { !$0.isEmpty }
            return Entry(
                item: item,
                names: names,
                terms: terms,
                words: terms.flatMap { $0.split(separator: " ").map(String.init) }
            )
        }
    }

    func search(_ query: String, limit: Int = 64) -> [EmojiItem] {
        let normalized = Self.normalize(query)
        guard !normalized.isEmpty, limit > 0 else { return [] }
        let queryWords = normalized.split(separator: " ").map(String.init)
        var buckets = Array(repeating: [EmojiItem](), count: 4)

        for entry in entries {
            guard let score = score(entry, query: normalized, words: queryWords) else { continue }
            buckets[score].append(entry.item)
        }
        return buckets.flatMap { $0 }.prefix(limit).map { $0 }
    }

    private func score(_ entry: Entry, query: String, words: [String]) -> Int? {
        if entry.names.contains(query) { return 0 }
        if entry.names.contains(where: { $0.hasPrefix(query) }) { return 1 }
        if words.allSatisfy({ word in entry.words.contains(where: { $0.hasPrefix(word) }) }) {
            return 2
        }
        if words.allSatisfy({ word in entry.terms.contains(where: { $0.contains(word) }) }) {
            return 3
        }
        return nil
    }

    static func normalize(_ value: String) -> String {
        let folded = value.folding(
            options: [.caseInsensitive, .diacriticInsensitive, .widthInsensitive],
            locale: Locale(identifier: "vi_VN")
        )
        let words = folded.unicodeScalars.split { scalar in
            !CharacterSet.alphanumerics.contains(scalar)
        }
        return words.map(String.init).joined(separator: " ")
    }
}
