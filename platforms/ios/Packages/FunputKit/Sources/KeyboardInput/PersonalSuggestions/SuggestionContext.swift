/// The words before the caret that the suggestion engine may read: the last one, and
/// the one before it when the two stand in a row.
///
/// They come from two places. A word typed through Funput is authored: the tracker saw
/// it end with a space, so the next word may be learned as following it. A word read
/// back from the document after the host moved the caret is borrowed: good enough to
/// ask the engine about, never to learn from.
struct SuggestionContext {
    /// The word the next one follows, or nil when nothing can vouch for one.
    private(set) var word: String?
    /// The word `word` stands after.
    private(set) var older: String?
    private(set) var borrowed = false

    /// `next` was just typed and is now the word before the caret; nil ends the run.
    mutating func follow(_ next: String?) {
        older = next == nil ? nil : word
        word = next
        borrowed = false
    }

    /// Takes the words `text` ends with, what the host shows before the caret. Any
    /// ending but a word and one space leaves nothing to follow: the caret is inside
    /// a word, after punctuation, after two spaces, or at the start of a line.
    mutating func borrow(from text: String?) {
        let found = text.flatMap(Self.words(before:))
        word = found?.last
        older = found?.older
        borrowed = found != nil
    }

    private static func words(before text: String) -> (last: String, older: String?)? {
        guard text.last == " ", let last = token(endingAt: text.dropLast()) else { return nil }
        let rest = text.dropLast(last.count + 1)
        guard rest.last == " " else { return (last, nil) }
        return (last, token(endingAt: rest.dropLast()))
    }

    /// The whole word `text` ends with, when it is one the tracker would learn.
    private static func token(endingAt text: Substring) -> String? {
        // One past the longest word is enough to tell that a run is too long.
        let run = text.reversed().prefix(AuthoredTokenTracker.maximumLength + 1)
        let length = run.prefix { $0.isLetter || $0.isCombiningMark }.count
        guard (AuthoredTokenTracker.minimumLength...AuthoredTokenTracker.maximumLength)
            .contains(length) else { return nil }
        return String(text.suffix(length))
    }
}

extension Character {
    /// A mark that joins the letter before it. Decomposed text carries its tones this way.
    var isCombiningMark: Bool {
        unicodeScalars.allSatisfy { scalar in
            switch scalar.properties.generalCategory {
            case .nonspacingMark, .spacingMark, .enclosingMark: true
            default: false
            }
        }
    }
}
