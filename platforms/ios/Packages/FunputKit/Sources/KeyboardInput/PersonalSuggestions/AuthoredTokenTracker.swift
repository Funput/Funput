struct AuthoredTokenTracker {
    static let minimumLength = 2
    static let maximumLength = 32

    private(set) var prefix = ""
    private(set) var completedToken: String?
    private var prefixLength = 0
    private var overflowCount = 0

    mutating func recordInsertion(_ text: String) {
        if text.utf8.count == 1, let byte = text.utf8.first {
            if byte >= 65 && byte <= 90 || byte >= 97 && byte <= 122 {
                append(text)
            } else {
                complete()
            }
            return
        }
        for character in text {
            if character.isLetter || isCombiningMark(character) && (!prefix.isEmpty || overflowCount > 0) {
                append(String(character))
            } else {
                complete()
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
        }
    }

    mutating func consume() -> KeyboardSuggestionInputUpdate {
        let update = KeyboardSuggestionInputUpdate(
            prefix: overflowCount == 0 ? prefix : "",
            completedToken: completedToken
        )
        completedToken = nil
        return update
    }

    mutating func reset() {
        prefix.removeAll(keepingCapacity: true)
        completedToken = nil
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

    private mutating func complete() {
        if overflowCount == 0, prefixLength >= Self.minimumLength {
            completedToken = prefix
        }
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

    public init(prefix: String, completedToken: String?) {
        self.prefix = prefix
        self.completedToken = completedToken
    }

    public static let empty = KeyboardSuggestionInputUpdate(prefix: "", completedToken: nil)
}
