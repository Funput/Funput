import Foundation

/// Per-app VI/EN resolution — the macOS port of the Windows shell's override map
/// (`platforms/windows/src/shell.rs`). A manual toggle is pinned per app for the
/// session and wins over the exclusion-list default on later focus changes, so
/// choosing EN no longer snaps back to VI the moment focus moves.
extension AppSettings {
    /// Decide the VI/EN target when focus lands on the app `front`, consuming a
    /// pending UI choice into the per-app map. Priority: pending UI choice →
    /// per-app pin → exclusion-list default. Returns `nil` when nothing applies
    /// (no pin and no exclusion list configured) — leave the current state as-is.
    func resolveVietnamese(for front: String?) -> Bool? {
        if let pending = pendingVietnameseOverride {
            pendingVietnameseOverride = nil
            if let front { vietnameseOverrides[front] = pending }
            return pending
        }
        if let front, let pinned = vietnameseOverrides[front] {
            return pinned
        }
        guard !excludedApps.isEmpty else { return nil }
        return !isExcluded(front)
    }

    /// Record a VI/EN choice made by the toggle hotkey. The hotkey fires while the
    /// target app is focused, so the choice pins to that app immediately; any stale
    /// pending UI choice is dropped.
    func pinVietnamese(_ on: Bool, to bundleId: String?) {
        vietnameseEnabled = on
        if let bundleId { vietnameseOverrides[bundleId] = on }
        pendingVietnameseOverride = nil
    }

    /// Record a VI/EN choice made from Funput's own UI (menu bar / Settings). Our
    /// window holds focus at that moment, so the choice binds to the next app the
    /// user returns to — otherwise the per-app default would revert it on refocus.
    func setVietnameseFromUI(_ on: Bool) {
        vietnameseEnabled = on
        pendingVietnameseOverride = on
    }
}
