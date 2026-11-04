import Foundation
import FunputShared
import Testing

/// The history has a ceiling that does not depend on what the user pastes.
struct ClipboardHistoryPolicyTests {
    private let maxItem = ClipboardHistoryPolicy.maxItemUTF16
    private let entriesThatFill = ClipboardHistoryPolicy.maxTotalUTF16 / ClipboardHistoryPolicy.maxItemUTF16

    @Test func entryAtTheLimitIsKeptAndOneUnitMoreIsNot() throws {
        let store = try makeStore()
        #expect(store.record(ClipboardItem(text: text("a", maxItem), sourceChangeCount: 1)))
        #expect(!store.record(ClipboardItem(text: text("b", maxItem + 1), sourceChangeCount: 2)))
        #expect(store.load().map(\.text.first) == ["a"])
    }

    /// Precomposed Vietnamese is one UTF-16 unit per letter, so the limit is about
    /// letters, not bytes.
    @Test func limitCountsUTF16UnitsNotBytes() {
        let vietnamese = String(repeating: "ạ", count: maxItem)
        #expect(vietnamese.utf8.count > maxItem)
        #expect(ClipboardHistoryPolicy.admits(vietnamese))
    }

    @Test func fullHistoryEvictsTheOldestUnpinnedEntry() throws {
        let store = try makeStore()
        for index in 0..<entriesThatFill {
            #expect(store.record(ClipboardItem(text: text("\(index)", maxItem), sourceChangeCount: index)))
        }
        #expect(store.load().count == entriesThatFill)
        #expect(store.record(ClipboardItem(text: text("mới", maxItem), sourceChangeCount: 99)))
        let items = store.load()
        #expect(items.count == entriesThatFill)
        #expect(items.first?.text.hasPrefix("mới") == true)
        #expect(!items.contains { $0.text.hasPrefix("0") })
        #expect(totalUnits(items) <= ClipboardHistoryPolicy.maxTotalUTF16)
    }

    /// Pinning is the user saying "keep this", so a new entry is what gives way.
    @Test func pinnedEntriesFillingTheBudgetTurnANewEntryAway() throws {
        let store = try makeStore()
        for index in 0..<entriesThatFill {
            #expect(store.record(ClipboardItem(
                text: text("\(index)", maxItem), isPinned: true, sourceChangeCount: index
            )))
        }
        #expect(!store.record(ClipboardItem(text: "ngắn", sourceChangeCount: 99)))
        let items = store.load()
        #expect(items.count == entriesThatFill)
        let allPinned = items.allSatisfy(\.isPinned)
        #expect(allPinned)
    }

    /// Written before the limits existed: the oversized entries, pinned or not, are
    /// gone on the next load and from disk on the next write.
    @Test func oversizedEntriesFromAnOlderBuildAreDropped() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let huge = text("x", maxItem + 1)
        let legacy = """
        {"items":[\
        {"id":"\(UUID())","text":"\(huge)","capturedAt":0,"isPinned":true,"sourceChangeCount":1},\
        {"id":"\(UUID())","text":"\(huge)y","capturedAt":0,"isPinned":false,"sourceChangeCount":2},\
        {"id":"\(UUID())","text":"giữ","capturedAt":0,"isPinned":true,"sourceChangeCount":3}]}
        """
        try Data(legacy.utf8).write(to: dir.appendingPathComponent("clipboard.json"))
        let store = ClipboardStore(directory: dir, expiry: .week)
        #expect(store.load().map(\.text) == ["giữ"])
    }

    private func text(_ prefix: String, _ units: Int) -> String {
        prefix + String(repeating: "a", count: units - prefix.utf16.count)
    }

    private func totalUnits(_ items: [ClipboardItem]) -> Int {
        items.reduce(0) { $0 + $1.text.utf16.count }
    }

    private func makeStore() throws -> ClipboardStore {
        ClipboardStore(directory: try makeDirectory(), expiry: .week)
    }

    private func makeDirectory() throws -> URL {
        let dir = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }
}
