public struct KeyboardSuggestionCandidate: Equatable, Sendable {
    public let text: String
    public let generation: UInt64

    public init(text: String, generation: UInt64) {
        self.text = text
        self.generation = generation
    }
}
