struct AuthoredTokenTracker {
    static let minimumLength = 2
    static let maximumLength = 32

    private(set) var prefix = ""
    private(set) var completedToken: String?
    /// The word the next one will be recorded as following, or nil when nothing
    /// here can vouch for what came before.
    ///
    /// This lives in the tracker because the tracker is the only thing that sees
    /// every mutation — which separator ended a word, when the caret left, when
    /// the prefix was abandoned. Anything downstream sees the result and not the
    /// reason.
    private(set) var context: String?
    private var prefixLength = 0
    private var overflowCount = 0

    mutating func recordInsertion(_ text: String) {
        if text.utf8.count == 1, let byte = text.utf8.first {
            if byte >= 65 && byte <= 90 || byte >= 97 && byte <= 122 {
                append(text)
            } else {
                complete(separator: Character(UnicodeScalar(byte)))
            }
            return
        }
        for character in text {
            if character.isLetter || isCombiningMark(character) && (!prefix.isEmpty || overflowCount > 0) {
                append(String(character))
            } else {
                complete(separator: character)
            }
        }
    }

    mutating func recordDeletion() {
        completedToken = nil
        if overflowCount > 0 {
            overflowCount -= 1
        } else if !prefix.isEmpty {
            prefix.removeLast()
            prefixLength -= 1
        } else {
            // Nothing left of this word to delete, so the caret has crossed back
            // over a boundary and what precedes it is no longer known.
            context = nil
        }
    }

    mutating func apply(_ mutations: [DocumentMutation]) {
        for mutation in mutations {
            switch mutation {
            case let .deleteBackward(count):
                for _ in 0..<count { recordDeletion() }
            case let .insert(text):
                recordInsertion(text)
            case .moveCursor:
                // The caret left the token being tracked; nothing typed here is a word any
                // more, so the prefix is abandoned rather than continued.
                reset()
            }
        }
    }

    mutating func consume() -> KeyboardSuggestionInputUpdate {
        let update = KeyboardSuggestionInputUpdate(
            prefix: overflowCount == 0 ? prefix : "",
            completedToken: completedToken,
            context: context
        )
        completedToken = nil
        return update
    }

    mutating func reset() {
        prefix.removeAll(keepingCapacity: true)
        completedToken = nil
        context = nil
        prefixLength = 0
        overflowCount = 0
    }

    private mutating func append(_ text: String) {
        completedToken = nil
        if prefixLength < Self.maximumLength, overflowCount == 0 {
            prefix.append(contentsOf: text)
            prefixLength += 1
        } else {
            overflowCount += 1
        }
    }

    /// A space continues the sentence, so the word just finished becomes the
    /// context for the next one. Anything else — a full stop, a newline, a comma
    /// — ends it, and two words either side of that were never adjacent. Guessing
    /// the other way would teach pairs nobody typed.
    private mutating func complete(separator: Character) {
        if overflowCount == 0, prefixLength >= Self.minimumLength {
            completedToken = prefix
        }
        context = separator == " " ? completedToken : nil
        prefix.removeAll(keepingCapacity: true)
        prefixLength = 0
        overflowCount = 0
    }

    private func isCombiningMark(_ character: Character) -> Bool {
        character.unicodeScalars.allSatisfy { scalar in
            switch scalar.properties.generalCategory {
            case .nonspacingMark, .spacingMark, .enclosingMark: true
            default: false
            }
        }
    }
}

public struct KeyboardSuggestionInputUpdate: Equatable, Sendable {
    public let prefix: String
    public let completedToken: String?
    /// The word `prefix` is being typed after, when one can be vouched for.
    public let context: String?

    public init(prefix: String, completedToken: String?, context: String? = nil) {
        self.prefix = prefix
        self.completedToken = completedToken
        self.context = context
    }

    public static let empty = KeyboardSuggestionInputUpdate(prefix: "", completedToken: nil)
}
