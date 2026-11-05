import Foundation

/// The pasted mark and the boot it belongs to, stored in `clipboard-marks.json`.
///
/// A few bytes on their own, so the keyboard can read the mark on its way up without
/// decoding the whole history: that read lands right when the user starts typing.
struct ClipboardMarks: Codable, Equatable {
    var bootTime: Int?
    var lastPastedChangeCount: Int?

    static let empty = ClipboardMarks(bootTime: nil, lastPastedChangeCount: nil)

    /// Claims the mark for this boot, dropping one the previous boot left: its
    /// change count names a generation that no longer exists and could collide.
    mutating func stampThisBoot() {
        if !marksAreCurrent { lastPastedChangeCount = nil }
        bootTime = ClipboardBootTime.current
    }

    /// Change counts restart at boot, so a mark written before this one names a
    /// generation that no longer exists.
    var marksAreCurrent: Bool {
        guard let bootTime, let current = ClipboardBootTime.current else { return false }
        return bootTime == current
    }
}
