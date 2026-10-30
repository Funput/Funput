import Foundation

/// How a word came to be in the dictionary.
public enum DictionaryOrigin: Sendable, Hashable {
    case learned
    case added
    case blocked
}

/// The four changes a person can make to one word.
public enum DictionaryEditKind: Sendable, Hashable, CaseIterable {
    /// Offered at once and never evicted. Lifts a block on the word.
    case add
    /// Dropped with what it learned; typing it again relearns it.
    case forget
    /// Dropped and never learned or offered again.
    case block
    case unblock
}

/// What an edit did, as the engine reports it.
public enum DictionaryEditOutcome: Sendable, Hashable {
    case applied
    /// The dictionary already looked like this.
    case unchanged
    /// Not a word the store can hold: empty, spaced or too long.
    case invalid
    /// The added-word or blocked-word list is full.
    case limitReached
}

/// The three lists the dictionary page switches between.
public enum DictionaryFilter: Sendable, Hashable, CaseIterable {
    case learned
    case added
    case blocked
}

public enum DictionarySort: Sendable, Hashable, CaseIterable {
    case mostUsed
    case recent
    /// Marks ignored first, so `ăn` sits beside `an` rather than after `z`.
    case alphabetical
}

public struct DictionaryWord: Sendable, Hashable, Identifiable {
    public let text: String
    public let uses: Int
    /// Comparable between words of one store; not a time.
    public let lastUsed: UInt64
    public let origin: DictionaryOrigin

    public var id: String { text }

    public init(text: String, uses: Int, lastUsed: UInt64, origin: DictionaryOrigin) {
        self.text = text
        self.uses = uses
        self.lastUsed = lastUsed
        self.origin = origin
    }
}

public struct DictionaryQuery: Sendable, Hashable {
    public var filter: DictionaryFilter
    public var sort: DictionarySort
    /// Matched anywhere in a word, marks and case ignored. Empty matches all.
    public var search: String

    public init(filter: DictionaryFilter = .learned, sort: DictionarySort = .mostUsed, search: String = "") {
        self.filter = filter
        self.sort = sort
        self.search = search
    }
}

/// One load of the dictionary page: the words of the chosen list, and how many
/// words each of the three lists holds regardless of the search.
public struct DictionaryPage: Sendable, Equatable {
    public var words: [DictionaryWord]
    public var counts: [DictionaryFilter: Int]

    public init(words: [DictionaryWord] = [], counts: [DictionaryFilter: Int] = [:]) {
        self.words = words
        self.counts = counts
    }

    public func count(of filter: DictionaryFilter) -> Int {
        counts[filter, default: 0]
    }
}

public enum DictionaryLimits {
    /// The longest word the store holds, in Unicode scalars. Pinned by a test to the
    /// engine's own `SUGGESTION_CHARS_CAP`, so it cannot drift from it.
    public static let maxWordLength = 32
}
