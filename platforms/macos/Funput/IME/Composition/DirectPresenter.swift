import Foundation

/// The composing word written into the document as ordinary text and rewritten in place
/// when a tone or shape mark lands. Nothing is marked, so nothing is underlined: typing
/// feels the way it does on Windows.
///
/// No document position is kept. Some clients renumber between keystrokes (Cursor's editor
/// answers from a hidden text area it rebuilds), so a rewrite is measured back from where
/// the caret is at that moment. Before rewriting, the presenter checks that the text right
/// before the caret is still the word it wrote. Anything else means the app or the user
/// changed the text, and the word is dropped rather than written over someone else's.
struct DirectPresenter: CompositionPresenter {
    /// What the document holds just before the caret: the last text written for this word.
    private(set) var shown = ""

    var writesIntoDocument: Bool { true }

    /// Letters added at the end are typed at the caret and need no document read; most
    /// keystrokes are those. Only a rewrite asks the client where the word is.
    mutating func show(_ text: String, on surface: CompositionSurface) -> CompositionOutcome {
        guard let edit = DirectEdit.edit(from: shown, to: text) else { return .shown }
        var range: NSRange?
        if edit.replacedTail > 0 {
            switch locate(on: surface) {
            case .found(let selection): range = DirectEdit.rewriteRange(edit, selection: selection)
            case .moved: return .dropped
            case .unknown: return .unsupported
            }
        }
        surface.insert(edit.text, replacing: range)
        shown = text
        return .shown
    }

    /// Writes the final form first (an expanded shortcut, a word restored to English), then
    /// the boundary. If that rewrite is refused, the document keeps the word as it was shown,
    /// and the boundary the user typed still lands.
    mutating func commit(_ word: String, boundary: String, on surface: CompositionSurface) {
        _ = show(word, on: surface)
        if !boundary.isEmpty { surface.insert(boundary, replacing: nil) }
    }

    mutating func reopen(_ word: String, replacing range: NSRange, on surface: CompositionSurface) {
        surface.insert(word, replacing: range)
        shown = word
    }

    private enum Location {
        case found(selection: NSRange)
        case moved
        case unknown
    }

    /// Where the word is now. It must end where the selection starts and read exactly
    /// `shown`. A caret the client cannot place is `unknown`; any other mismatch is `moved`.
    private func locate(on surface: CompositionSurface) -> Location {
        guard let selection = surface.selection() else { return .unknown }
        guard let word = DirectEdit.wordRange(length: shown.utf16.count, selection: selection),
            surface.text(in: word) == shown
        else { return .moved }
        return .found(selection: selection)
    }
}
