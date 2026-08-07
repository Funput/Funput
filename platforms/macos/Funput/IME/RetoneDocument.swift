import InputMethodKit

/// The focused app's text, as retoning needs to see it: where the caret is, and what sits
/// just before it.
///
/// Reading is the part that varies by app — a client is free to answer with nothing — so it
/// sits behind this protocol, which also lets `Retone` be exercised against a plain string
/// in tests. Writing needs no seam: it is one `setMarkedText` back in the controller.
protocol RetoneDocument {
    /// Where the caret is, in UTF-16 units, or nil when the client cannot place it.
    func caret() -> Int?

    /// The text in `range`, or nil when the client will not hand it over. Implementations
    /// pass on whatever they are given; `Retone` checks it covers the range it asked for.
    func text(in range: NSRange) -> String?
}

/// An `IMKTextInput` client seen through that lens — the only place in Funput that reads
/// the focused app's document.
///
/// Every call here is a synchronous round trip to another process, which is why the only
/// caller is the Backspace path: on macOS 26/27 those round trips are exactly what
/// Chromium's text-input bridge deadlocks on (see `docs/KNOWN_ISSUES.md`), so they must
/// never reach the per-keystroke path.
struct IMKRetoneDocument: RetoneDocument {
    let client: IMKTextInput

    func caret() -> Int? {
        Retone.caret(selected: client.selectedRange(), marked: client.markedRange())
    }

    func text(in range: NSRange) -> String? {
        client.attributedSubstring(from: range)?.string
    }
}
