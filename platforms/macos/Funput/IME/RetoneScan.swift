import Foundation

/// Re-opening a finished word after Backspace: `chào` + Space + ⌫ then `s` gives `cháo`.
///
/// The engine decides *whether* — `FunputComposer.adopt` refuses anything that is not a
/// complete Vietnamese syllable, which keeps English words and URLs literal. This type only
/// works out *which* word the caret is about to land on, and which stretch of the document
/// has to be handed over for it.
///
/// Windows answers the same question from a shadow copy of everything Funput typed
/// (`funput_desktop::CommittedTail`), because a hook shell has no document to ask. macOS
/// does, so there is no shadow here to keep in step with the caret — and no invariant to
/// break when the user clicks somewhere else.
enum Retone {
    /// How far back to look for the start of the word. The longest Vietnamese syllable
    /// (`nghiêng`) is 7 characters; the slack matches Android's `WordLookback`.
    static let lookback = 12

    /// The single edit that turns a committed word back into a live composition.
    struct Plan: Equatable {
        /// The word to compose, precomposed (NFC) the way the engine hands text back.
        let word: String
        /// The stretch of document it replaces: the word *plus* the character Backspace
        /// was about to delete, so one edit does both and nothing can land in between.
        let replacement: NSRange
    }

    /// What Backspace should do, or nil to let the app delete a character the ordinary way.
    ///
    /// `adopt` is the engine, and it is asked last — only ever about a word that is really
    /// there — so a refusal leaves the document exactly as it was.
    static func plan(document: RetoneDocument, adopt: (String) -> Bool) -> Plan? {
        // `end` is the character Backspace removes; with nothing in front of it there is no
        // word to re-open, and asking the client to prove that costs a round trip.
        guard let caret = document.caret(), caret > 1 else { return nil }
        let end = caret - 1
        let start = max(0, end - lookback)
        // Everything below is anchored on the window ending exactly at `end`. A client that
        // answers with some other stretch of text gives no way to tell which end it trimmed,
        // and a range built on the wrong guess would replace text Funput never wrote.
        guard let window = document.text(in: NSRange(location: start, length: end - start)),
            window.utf16.count == end - start,
            let raw = trailingWord(of: window, clipped: start > 0)
        else { return nil }

        let word = raw.precomposedStringWithCanonicalMapping
        guard adopt(word) else { return nil }
        // The range is measured on `raw`, not on `word`: a document is free to hold the
        // syllable decomposed, where it spans more UTF-16 units than the precomposed form
        // about to replace it.
        let length = raw.utf16.count
        return Plan(word: word, replacement: NSRange(location: end - length, length: length + 1))
    }

    /// The unbroken run of word characters at the end of `text`, or nil when there is no
    /// whole word to re-open.
    ///
    /// Two ways that happens: `text` ends on a separator, where the caret lands between
    /// words; or the run reaches the front of a `clipped` window, where the word carries on
    /// out of sight and only a fragment of it would be handed over.
    private static func trailingWord(of text: String, clipped: Bool) -> String? {
        var start = text.endIndex
        while start > text.startIndex {
            let previous = text.index(before: start)
            if let scalar = text[previous].unicodeScalars.first, isSeparator(scalar) { break }
            start = previous
        }
        if start == text.startIndex, clipped { return nil }
        return start == text.endIndex ? nil : String(text[start...])
    }

    /// Mirrors the engine's own word-boundary rule via `InputEventPolicy`. `.telex` is
    /// passed deliberately: Telex-advanced's `[` and `]` compose while *typing*, but text
    /// already sitting in the document is just text, and there a bracket ends a word — the
    /// same reading `funput_desktop`'s `is_separator` settled on.
    private static func isSeparator(_ scalar: Unicode.Scalar) -> Bool {
        InputEventPolicy.isBoundary(scalar, method: .telex)
    }
}
