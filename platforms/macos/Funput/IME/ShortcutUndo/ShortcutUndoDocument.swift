import Foundation

/// Where the text the last gõ tắt expansion wrote sits in the focused document.
///
/// The engine remembers the expansion, but only the document can say it is still right
/// before the caret: the app may have changed the text, or the caret may have moved. A
/// client that cannot show it gets no undo, and Backspace deletes a character as usual.
/// Reads cost a round trip to the app each, so they run only when an expansion lands and
/// on Backspace while one is remembered.
enum ShortcutUndoDocument {
    /// The caret, when the client applies replacement ranges and places a caret with
    /// nothing selected and no marked text of its own (the same reading retone relies on,
    /// `Retone.caret`); nil otherwise.
    static func caret(on surface: CompositionSurface) -> Int? {
        guard surface.supportsDocumentAccess else { return nil }
        let none = NSRange(location: NSNotFound, length: 0)
        return Retone.caret(selected: surface.selection() ?? none, marked: surface.markedRange() ?? none)
    }

    /// The range `written` fills right before the caret, or nil unless that is certain:
    /// the caret reads as above, and the client hands back exactly `written` from in front
    /// of it.
    static func range(of written: String, on surface: CompositionSurface) -> NSRange? {
        let length = written.utf16.count
        guard length > 0, let caret = caret(on: surface), caret >= length else { return nil }
        let range = NSRange(location: caret - length, length: length)
        return surface.text(in: range) == written ? range : nil
    }
}
