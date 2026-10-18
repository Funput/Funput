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
        guard edit.replacedTail > 0 else {
            surface.insert(edit.text, replacing: nil)
            shown = text
            return .shown
        }
        switch Self.locate(shown, on: surface) {
        case .found(let selection):
            surface.insert(edit.text, replacing: DirectEdit.rewriteRange(edit, selection: selection))
            shown = text
            return .rewritten
        case .moved: return .dropped
        case .unknown: return .unsupported
        }
    }

    /// Writes the final form first (an expanded shortcut, a word restored to English), then
    /// the boundary. If that rewrite is refused, the document keeps the word as it was shown,
    /// and the boundary the user typed still lands.
    mutating func commit(_ word: String, boundary: String, on surface: CompositionSurface) {
        _ = show(word, on: surface)
        if !boundary.isEmpty { surface.insert(boundary, replacing: nil) }
    }

    /// Backspace is never answered with a write. Chromium forwards the original key to the
    /// page whenever an input method writes through a replacement range, so a rewrite here
    /// would be followed by the page deleting one more character. The app deletes the last
    /// character itself; if the engine changed more than that (a tone moving back as a final
    /// consonant goes), the next key's `show` reconciles the document.
    mutating func backspace(to text: String, on surface: CompositionSurface) -> Bool {
        shown = String(shown.dropLast())
        return false
    }

    /// The word is already in the document, right before the character Backspace is about to
    /// delete, so nothing is written: the app deletes that character and the word is live.
    mutating func reopen(_ word: String, replacing range: NSRange, on surface: CompositionSurface) -> Bool {
        shown = word
        return false
    }

    enum Location: Equatable {
        case found(selection: NSRange)
        case moved
        case unknown
    }

    /// Where `text` is, as the client tells it: it must end where the selection starts and
    /// read exactly `text`. A caret the client cannot place is `unknown`; any other mismatch,
    /// including a caret too close to the start to hold the text, is `moved`.
    static func locate(_ text: String, on surface: CompositionSurface) -> Location {
        guard let selection = surface.selection() else { return .unknown }
        guard let range = DirectEdit.wordRange(length: text.utf16.count, selection: selection),
            surface.text(in: range) == text
        else { return .moved }
        return .found(selection: selection)
    }

    /// Whether the client hands over the character right before its caret: the proof,
    /// asked before anything is written, that its answers describe a real document.
    static func readsBeforeCaret(on surface: CompositionSurface) -> Bool {
        guard let selection = surface.selection(), selection.length == 0, selection.location > 0,
            let before = surface.text(in: NSRange(location: selection.location - 1, length: 1))
        else { return false }
        return !before.isEmpty
    }
}

/// The arithmetic behind `DirectPresenter`: which part of a word to rewrite, and where in
/// the document that part sits. Pure, so every offset is pinned by tests in UTF-16 units.
enum DirectEdit {
    /// One write to the document: replace the last `replacedTail` UTF-16 units of the word
    /// with `text`. Zero types `text` at the caret, exactly like an ordinary keystroke.
    struct Edit: Equatable {
        let text: String
        let replacedTail: Int
    }

    /// The smallest write turning `shown` into `new`, or nil when they are equal.
    /// Compared by `Character`, so a tone mark is never split from its vowel.
    static func edit(from shown: String, to new: String) -> Edit? {
        guard shown != new else { return nil }
        let common = zip(shown, new).prefix { $0 == $1 }.count
        let kept = shown.prefix(common).utf16.count
        return Edit(text: String(new.dropFirst(common)), replacedTail: shown.utf16.count - kept)
    }

    /// Where a word of `length` UTF-16 units sits, given the client's selection: it ends
    /// where the selection starts. Nil when the caret is too close to the start to hold it.
    static func wordRange(length: Int, selection: NSRange) -> NSRange? {
        guard selection.location != NSNotFound, selection.location >= length else { return nil }
        return NSRange(location: selection.location - length, length: length)
    }

    /// The document range a rewrite replaces: the word's changed tail, plus any inline
    /// suggestion selected after it (Excel, URL bars, Spotlight). Typing would replace that
    /// suggestion, so the rewrite does too and the app suggests afresh.
    static func rewriteRange(_ edit: Edit, selection: NSRange) -> NSRange {
        NSRange(
            location: selection.location - edit.replacedTail,
            length: edit.replacedTail + selection.length
        )
    }
}
