import AppKit
import InputMethodKit

/// What a presenter needs from the focused text field to put the word being composed on
/// screen. `IMKCompositionSurface` is the real one; tests drive presenters through a fake,
/// so every write a presenter makes is checked without a live app.
protocol CompositionSurface {
    /// Type `text` over `range`, or over the selection at the caret when `range` is nil.
    /// The text lands as ordinary text, with no marking.
    func insert(_ text: String, replacing range: NSRange?)

    /// Show `text` as marked text over `range`, or at the caret when `range` is nil.
    func mark(_ text: String, replacing range: NSRange?)

    /// The client's selection, or nil when it cannot say where its caret is.
    func selection() -> NSRange?

    /// The document text in `range`, or nil when the client will not hand it over.
    func text(in range: NSRange) -> String?

    /// Whether the client honours `replacing:` on `insert`.
    var supportsDocumentAccess: Bool { get }
}

/// An `IMKTextInput` client as a composition surface: the one place that writes the word
/// being composed into the focused app.
///
/// Every read here is a synchronous round trip to that app's process, and on macOS 26/27
/// Chromium's text-input bridge is known to stall on those (see `docs/KNOWN_ISSUES.md`).
/// Presenters therefore read only when a word has to be rewritten, never per keystroke.
struct IMKCompositionSurface: CompositionSurface {
    let client: IMKTextInput

    func insert(_ text: String, replacing range: NSRange?) {
        client.insertText(text, replacementRange: range ?? Self.atCaret)
    }

    func mark(_ text: String, replacing range: NSRange?) {
        let marked = NSAttributedString(string: text, attributes: [
            .underlineStyle: NSUnderlineStyle.single.rawValue,
            .underlineColor: NSColor.labelColor,
        ])
        client.setMarkedText(
            marked,
            selectionRange: NSRange(location: text.utf16.count, length: 0),
            replacementRange: range ?? Self.atCaret
        )
    }

    func selection() -> NSRange? {
        let range = client.selectedRange()
        return range.location == NSNotFound ? nil : range
    }

    func text(in range: NSRange) -> String? {
        client.attributedSubstring(from: range)?.string
    }

    /// Only clients speaking TSM's document-access protocol apply a replacement range;
    /// the others ignore it (see `IMKTextInput.insertText(_:replacementRange:)`).
    var supportsDocumentAccess: Bool {
        client.supportsProperty(TSMDocumentPropertyTag(kTSMDocumentSupportDocumentAccessPropertyTag))
    }

    /// The replacement range IMKit reads as "wherever the caret is".
    private static let atCaret = NSRange(location: NSNotFound, length: 0)
}
