import Foundation

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
