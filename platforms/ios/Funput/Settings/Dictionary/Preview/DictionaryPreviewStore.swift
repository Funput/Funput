#if DEBUG
import Foundation
import PersonalDictionary

/// An in-memory dictionary for previews and UI tests, following the engine's rules
/// closely enough to exercise every state of the page: blocking forgets, adding
/// lifts a block, and a spaced word is refused.
actor DictionaryPreviewStore: PersonalDictionaryStoring {
    static let samples: [DictionaryWord] = [
        ("được", 214), ("không", 180), ("mình", 96), ("Zalo", 37), ("deadline", 12), ("hoà", 9),
    ].enumerated().map { index, sample in
        DictionaryWord(text: sample.0.lowercased(), uses: sample.1, lastUsed: UInt64(index), origin: .learned)
    } + [DictionaryWord(text: "funput", uses: 2, lastUsed: 9, origin: .added)]

    private var words: [DictionaryWord]
    private var blocked: [String] = ["đm"]
    private var sequence: UInt64 = 10

    init(words: [DictionaryWord] = DictionaryPreviewStore.samples) {
        self.words = words
        if words.isEmpty { blocked = [] }
    }

    func load(_ query: DictionaryQuery) -> DictionaryPage {
        let counts: [DictionaryFilter: Int] = [
            .learned: words.filter { $0.origin == .learned }.count,
            .added: words.filter { $0.origin == .added }.count,
            .blocked: blocked.count,
        ]
        return DictionaryPage(words: listed(query), counts: counts)
    }

    func apply(_ kind: DictionaryEditKind, word: String) -> DictionaryEditOutcome {
        let key = word.lowercased()
        guard !key.isEmpty, !key.contains(where: \.isWhitespace) else { return .invalid }
        sequence += 1
        switch kind {
        case .add:
            if words.contains(where: { $0.text == key && $0.origin == .added }) { return .unchanged }
            words.removeAll { $0.text == key }
            blocked.removeAll { $0 == key }
            words.append(DictionaryWord(text: key, uses: 2, lastUsed: sequence, origin: .added))
        case .forget:
            guard words.contains(where: { $0.text == key }) else { return .unchanged }
            words.removeAll { $0.text == key }
        case .block:
            guard !blocked.contains(key) else { return .unchanged }
            words.removeAll { $0.text == key }
            blocked.append(key)
        case .unblock:
            guard blocked.contains(key) else { return .unchanged }
            blocked.removeAll { $0 == key }
        }
        return .applied
    }

    private func listed(_ query: DictionaryQuery) -> [DictionaryWord] {
        let pool = switch query.filter {
        case .learned: words.filter { $0.origin == .learned }
        case .added: words.filter { $0.origin == .added }
        case .blocked: blocked.map { DictionaryWord(text: $0, uses: 0, lastUsed: 0, origin: .blocked) }
        }
        let needle = DictionarySearchKey(query.search)
        let matching = needle.isEmpty ? pool : pool.filter { DictionarySearchKey($0.text).contains(needle) }
        return switch query.sort {
        case .mostUsed: matching.sorted { ($0.uses, $0.lastUsed) > ($1.uses, $1.lastUsed) }
        case .recent: matching.sorted { $0.lastUsed > $1.lastUsed }
        case .alphabetical: matching.sorted { $0.text.localizedStandardCompare($1.text) == .orderedAscending }
        }
    }
}
#endif
