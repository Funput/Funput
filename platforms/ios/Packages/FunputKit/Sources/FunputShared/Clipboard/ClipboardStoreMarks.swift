import Foundation

/// What the keyboard remembers about the *current* pasteboard between sessions.
///
/// Kept apart from the history itself because the lifetime differs: entries are the
/// user's data, while this number exists only to keep the toolbar from inviting a
/// paste the user already made. The keyboard is rebuilt in every app it appears in,
/// so the answer cannot live in the session.
public extension ClipboardStore {
    /// Already pasted: the offer stays down until the user copies something else.
    /// Pasting the same thing again is what the history panel is for. Does not
    /// survive a reboot, where the number means something else.
    func lastPastedChangeCount() -> Int? {
        let payload = read()
        return payload.marksAreCurrent ? payload.lastPastedChangeCount : nil
    }

    @discardableResult
    func markPasted(_ changeCount: Int) -> Bool {
        var payload = read()
        guard !payload.marksAreCurrent || payload.lastPastedChangeCount != changeCount else {
            return true
        }
        payload.stampThisBoot()
        payload.lastPastedChangeCount = changeCount
        return write(payload)
    }
}
