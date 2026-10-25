public struct KeyboardSuggestionCandidate: Equatable, Sendable {
    /// What a slot on the bar stands for, which decides how it is drawn and what a
    /// tap on it does.
    public enum Kind: String, Sendable {
        /// A word the user is likely typing; tapping completes it.
        case suggestion
        /// The word as typed, shown in quotes beside its repair; tapping keeps it.
        case correctionTyped
        /// A repair the engine can reach; tapping commits it.
        case correctionCandidate
        /// The word a repair just replaced; tapping puts it back.
        case correctionUndo
        /// The word a gõ tắt expansion just replaced; tapping puts it back.
        case shortcutUndo
    }

    public let text: String
    public let generation: UInt64
    public let kind: Kind

    public init(text: String, generation: UInt64, kind: Kind = .suggestion) {
        self.text = text
        self.generation = generation
        self.kind = kind
    }

    /// The slot's title. The typed word sits in quotes so it reads as a thing kept
    /// rather than a thing offered, and the undo chip leads with its arrow.
    var barTitle: String {
        switch kind {
        case .suggestion, .correctionCandidate: text
        case .correctionTyped: "\u{201C}\(text)\u{201D}"
        case .correctionUndo, .shortcutUndo: "\u{21A9} \(text)"
        }
    }

    var accessibilityLabel: String {
        switch kind {
        case .suggestion: "Gợi ý, \(text)"
        case .correctionTyped: "Giữ nguyên, \(text)"
        case .correctionCandidate: "Sửa thành, \(text)"
        case .correctionUndo: "Hoàn tác, \(text)"
        case .shortcutUndo: "Hoàn tác gõ tắt, \(text)"
        }
    }
}
