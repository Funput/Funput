import Foundation
@testable import KeyboardRenderer
import Testing

@Suite("Clipboard history sections")
struct ClipboardHistorySectionTests {
    private let epoch = Date(timeIntervalSince1970: 1_800_000_000)

    @Test("Pinned entries lead, and each group runs newest first")
    func ordering() {
        let groups = ClipboardHistorySection.make(from: [
            entry("cũ", at: 0),
            entry("mới", at: 100),
            entry("ghim cũ", at: 10, pinned: true),
            entry("ghim mới", at: 50, pinned: true),
        ])

        #expect(groups.map(\.title) == [
            ClipboardHistorySection.pinnedTitle,
            ClipboardHistorySection.recentTitle,
        ])
        #expect(groups[0].entries.map(\.text) == ["ghim mới", "ghim cũ"])
        #expect(groups[1].entries.map(\.text) == ["mới", "cũ"])
    }

    @Test("A group with nothing in it disappears instead of showing a bare heading")
    func dropsEmptyGroups() {
        let onlyRecent = ClipboardHistorySection.make(from: [entry("a", at: 0)])
        #expect(onlyRecent.map(\.title) == [ClipboardHistorySection.recentTitle])

        let onlyPinned = ClipboardHistorySection.make(from: [entry("a", at: 0, pinned: true)])
        #expect(onlyPinned.map(\.title) == [ClipboardHistorySection.pinnedTitle])
    }

    @Test("No entries means no groups at all")
    func empty() {
        #expect(ClipboardHistorySection.make(from: []).isEmpty)
    }

    private func entry(_ text: String, at offset: TimeInterval, pinned: Bool = false)
        -> KeyboardClipboardEntry {
        KeyboardClipboardEntry(
            id: UUID(),
            text: text,
            capturedAt: epoch.addingTimeInterval(offset),
            isPinned: pinned
        )
    }
}
