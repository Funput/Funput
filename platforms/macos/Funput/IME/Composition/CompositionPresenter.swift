import Foundation

/// What became of a word a presenter was asked to show.
enum CompositionOutcome: Equatable {
    case shown
    /// Shown by rewriting text the client first confirmed was the word: proof that the
    /// client's answers describe its document (see `ClientTrust`).
    case rewritten
    /// The word could not be updated safely and was given up. The document is unchanged.
    case dropped
    /// The client cannot say where its caret is, so this presenter cannot work there.
    case unsupported
}

/// One way of putting the word being composed into the focused app.
///
/// `CompositionSession` picks a presenter when a word starts, and the word keeps it until
/// it is committed. A new way of presenting text is a new conformance plus one line in the
/// session's choice; nothing in the controller changes.
protocol CompositionPresenter {
    /// True when the composing text sits in the document as ordinary text, so the client
    /// knows nothing of it: neither a commit nor a click reaches IMKit on its behalf.
    var writesIntoDocument: Bool { get }

    /// The text last shown for this word.
    var shown: String { get }

    /// Show `text`, possibly empty, as the word being composed.
    mutating func show(_ text: String, on surface: CompositionSurface) -> CompositionOutcome

    /// End the word: the document must now read `word` followed by `boundary`.
    mutating func commit(_ word: String, boundary: String, on surface: CompositionSurface)

    /// Backspace took the last character off the word in the engine, leaving `text`.
    /// Returns whether Funput consumed the key; false lets the app delete the character.
    mutating func backspace(to text: String, on surface: CompositionSurface) -> Bool

    /// Backspace is about to make a committed word the live composition again: `range`
    /// covers the word and the character being deleted. Returns whether Funput consumed
    /// the key; false lets the app delete that character itself.
    mutating func reopen(_ word: String, replacing range: NSRange, on surface: CompositionSurface) -> Bool
}

/// The composing word as marked text, which every client underlines: the way Funput has
/// always typed, and the fallback wherever writing into the document is off or unsupported.
struct MarkedPresenter: CompositionPresenter {
    private(set) var shown = ""

    var writesIntoDocument: Bool { false }

    mutating func show(_ text: String, on surface: CompositionSurface) -> CompositionOutcome {
        surface.mark(text, replacing: nil)
        shown = text
        return .shown
    }

    /// Marked text is on screen for as long as this word is, so the insert always runs:
    /// it replaces the marked text, and an empty one simply clears it.
    func commit(_ word: String, boundary: String, on surface: CompositionSurface) {
        surface.insert(word + boundary, replacing: nil)
    }

    mutating func backspace(to text: String, on surface: CompositionSurface) -> Bool {
        _ = show(text, on: surface)
        return true
    }

    /// One `setMarkedText` replaces the word and the deleted character, so the app must
    /// not delete again.
    mutating func reopen(_ word: String, replacing range: NSRange, on surface: CompositionSurface) -> Bool {
        surface.mark(word, replacing: range)
        shown = word
        return true
    }
}
