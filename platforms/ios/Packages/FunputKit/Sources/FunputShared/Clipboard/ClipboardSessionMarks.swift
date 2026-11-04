import Foundation

/// What a new keyboard session inherits from the previous one.
///
/// Split from the paste recorder because the lifetime is the point: the recorder is
/// rebuilt whenever the keyboard appears in another app, while this answer has to
/// outlive it. A protocol, so a test can stand in for the store.
public protocol ClipboardSessionMarkStoring {
    /// nil means nothing from this boot has been pasted through Funput yet.
    func lastPastedChangeCount() -> Int?
    @discardableResult
    func markPasted(_ changeCount: Int) -> Bool
}

extension ClipboardStore: ClipboardSessionMarkStoring {}
