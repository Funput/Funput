import Foundation

/// What a new keyboard session inherits from the previous one.
///
/// Split from the capture controller because the lifetime is the point: the
/// controller is rebuilt whenever the keyboard appears in another app, while these
/// answers have to outlive it. Three calls, so a test can stand in for the store.
public protocol ClipboardSessionMarkStoring {
    /// nil means "read the pasteboard to find out", which costs the user a banner.
    func lastCapturedChangeCount() -> Int?
    func lastPastedChangeCount() -> Int?
    @discardableResult
    func markPasted(_ changeCount: Int) -> Bool
}

extension ClipboardStore: ClipboardSessionMarkStoring {}
