import Foundation

/// Where the text the last gõ tắt expansion wrote sits in the focused document.
///
/// The engine arms the undo, but only the document can say the expansion is still right
/// before the caret: the app may have changed the text, or the caret may have moved. A
/// client that cannot show it gets no undo, and Backspace deletes a character as usual.
/// Reads cost a round trip to the app each, so this runs only on Backspace, and only
/// while an undo is armed.
enum ShortcutUndoDocument {
    /// The range `written` fills right before the caret, or nil unless that is certain:
    /// the client applies replacement ranges, places a caret with nothing selected and no
    /// marked text of its own (the same reading retone relies on, `Retone.caret`), and
    /// hands back exactly `written` from in front of it.
    static func range(of written: String, on surface: CompositionSurface) -> NSRange? {
        let length = written.utf16.count
        guard length > 0, surface.supportsDocumentAccess else { return nil }
        let none = NSRange(location: NSNotFound, length: 0)
        guard let caret = Retone.caret(
            selected: surface.selection() ?? none,
            marked: surface.markedRange() ?? none
        ), caret >= length
        else { return nil }
        let range = NSRange(location: caret - length, length: length)
        return surface.text(in: range) == written ? range : nil
    }
}
