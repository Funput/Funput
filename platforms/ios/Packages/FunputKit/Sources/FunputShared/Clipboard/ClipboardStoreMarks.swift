import Foundation

/// What the keyboard remembers about the *current* pasteboard between sessions.
///
/// Kept apart from the history itself because the lifetime differs: entries are the
/// user's data, while these two numbers exist only to spare the next session work
/// the user would notice — a pasteboard read raises a system paste banner, and the
/// keyboard is rebuilt in every app it appears in.
public extension ClipboardStore {
    /// The generation whose text is already stored, so reopening the keyboard on an
    /// unchanged pasteboard reads nothing. Survives the entry expiring or being
    /// deleted; does not survive a reboot, where the number means something else.
    func lastCapturedChangeCount() -> Int? {
        let payload = read()
        return payload.marksAreCurrent ? payload.lastCapturedChangeCount : nil
    }

    /// Already pasted: the offer stays down until the user copies something else.
    /// Pasting the same thing again is what the history panel is for.
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
