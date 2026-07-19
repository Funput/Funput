public struct PersonalSuggestionCandidate: Equatable, Sendable {
    public let text: String

    public init(text: String) {
        self.text = text
    }
}

public struct PersonalSuggestionStats: Equatable, Sendable {
    public let words: Int
    public let promotedWords: Int
    public let exactNodes: Int
    public let foldedNodes: Int
    public let pendingMutations: Int
    public let journalBytes: UInt64
    public let estimatedHeapBytes: UInt64
    public let lastSnapshotBytes: UInt64
}
