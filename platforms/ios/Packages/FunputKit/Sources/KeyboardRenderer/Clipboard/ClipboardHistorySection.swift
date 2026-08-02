import Foundation

/// Groups clipboard history into the two sections the panel shows.
///
/// A pure function so the ordering rules can be held to account by a unit test
/// without a device — the same seam that made ``ClipboardOfferPolicy`` testable.
enum ClipboardHistorySection {
    struct Group: Equatable {
        let title: String
        let entries: [KeyboardClipboardEntry]
    }

    static let pinnedTitle = "Đã ghim"
    static let recentTitle = "Gần đây"

    /// Pinned first, newest first inside each group, and an empty group is dropped
    /// rather than shown as a bare heading.
    static func make(from entries: [KeyboardClipboardEntry]) -> [Group] {
        let sorted = entries.sorted { $0.capturedAt > $1.capturedAt }
        return [
            Group(title: pinnedTitle, entries: sorted.filter(\.isPinned)),
            Group(title: recentTitle, entries: sorted.filter { !$0.isPinned }),
        ].filter { !$0.entries.isEmpty }
    }
}
