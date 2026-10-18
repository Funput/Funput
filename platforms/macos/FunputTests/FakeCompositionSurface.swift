import Foundation
@testable import Funput

/// A text field in memory. It applies writes the way a client would (typing at the caret
/// replaces the selection or the marked text) and records every call, so tests pin the
/// exact traffic a presenter sends to IMKit, reads included.
final class FakeCompositionSurface: CompositionSurface {
    enum Call: Equatable {
        case insert(String, NSRange?)
        case mark(String, NSRange?)
        case selection
        case text(NSRange)
        case supportsDocumentAccess
    }

    private(set) var calls: [Call] = []
    private(set) var document = ""
    /// Where the caret is, and how much text after it is selected (an inline suggestion).
    var caret = 0
    var selectionLength = 0
    /// False makes `selection()` answer nil, like a client that cannot place its caret.
    var placesCaret = true
    var acceptsReplacementRanges = true
    /// Behave like Warp's view: claim document access, answer `(0, 0)` for the caret, hand
    /// over no text, and ignore replacement ranges.
    var blind = false
    /// Positions are reported relative to this offset, the way Cursor's editor numbers
    /// from the start of a hidden text area it rebuilds between keystrokes.
    var windowStart = 0
    var clientID: String? = "app.funput.tests.client"
    private var marked: NSRange?

    init(document: String = "") {
        self.document = document
        caret = document.utf16.count
    }

    var reads: [Call] {
        calls.filter { if case .selection = $0 { true } else if case .text = $0 { true } else { false } }
    }

    func insert(_ text: String, replacing range: NSRange?) {
        calls.append(.insert(text, range))
        write(text, over: blind ? nil : range)
        marked = nil
    }

    func mark(_ text: String, replacing range: NSRange?) {
        calls.append(.mark(text, range))
        let start = write(text, over: blind ? nil : range)
        marked = text.isEmpty ? nil : NSRange(location: start, length: text.utf16.count)
    }

    func selection() -> NSRange? {
        calls.append(.selection)
        if blind { return NSRange(location: 0, length: 0) }
        return placesCaret ? NSRange(location: caret - windowStart, length: selectionLength) : nil
    }

    func text(in range: NSRange) -> String? {
        calls.append(.text(range))
        let absolute = NSRange(location: range.location + windowStart, length: range.length)
        guard !blind, absolute.location >= 0, NSMaxRange(absolute) <= document.utf16.count else { return nil }
        return (document as NSString).substring(with: absolute)
    }

    var supportsDocumentAccess: Bool {
        calls.append(.supportsDocumentAccess)
        return acceptsReplacementRanges
    }

    /// Simulate the user, or the app, editing the document behind Funput's back.
    func replaceDocument(_ text: String) {
        document = text
        caret = text.utf16.count
        selectionLength = 0
        marked = nil
    }

    func clearCalls() {
        calls.removeAll()
    }

    /// Replace `range` (shifted into document positions), or else the marked text, or else
    /// the selection, with `text`; returns where the text starts.
    @discardableResult
    private func write(_ text: String, over range: NSRange?) -> Int {
        let target = range.map { NSRange(location: $0.location + windowStart, length: $0.length) }
            ?? marked ?? NSRange(location: caret, length: selectionLength)
        document = (document as NSString).replacingCharacters(in: target, with: text)
        caret = target.location + text.utf16.count
        selectionLength = 0
        return target.location
    }
}
