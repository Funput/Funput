import Foundation
import FunputShared
import Testing

struct ClipboardRecordStoreTests {
    @Test func repasteKeepsIdentityAndPinAndMovesToFront() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let store = ClipboardStore(directory: dir)
        let pinned = ClipboardItem(text: "pin", isPinned: true, sourceChangeCount: 1)
        #expect(store.record(pinned))
        #expect(store.record(ClipboardItem(text: "other", sourceChangeCount: 2)))
        #expect(store.record(ClipboardItem(text: "pin", sourceChangeCount: 3)))
        let items = store.load()
        #expect(items.map(\.text) == ["pin", "other"])
        #expect(items.first?.id == pinned.id)
        #expect(items.first?.isPinned == true)
        let bytes = try Data(contentsOf: dir.appendingPathComponent("clipboard.json"))
        #expect(store.record(ClipboardItem(text: "pin", sourceChangeCount: 3)))
        #expect(try Data(contentsOf: dir.appendingPathComponent("clipboard.json")) == bytes)
    }

    @Test func sameCountWithDifferentTextAfterRestartIsKept() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let store = ClipboardStore(directory: dir)
        #expect(store.record(ClipboardItem(text: "before reboot", sourceChangeCount: 1)))
        #expect(store.record(ClipboardItem(text: "after reboot", sourceChangeCount: 1)))
        #expect(store.load().count == 2)
    }

    /// History written while the keyboard still captured on its own: the same file,
    /// with a captured mark nothing reads anymore and change counts from a past boot.
    @Test func historyWrittenByAnOlderBuildStillLoadsAndAcceptsPastes() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let legacy = """
        {"lastCapturedChangeCount":4,"items":[{"id":"\(UUID())","text":"ghim cu",\
        "capturedAt":0,"isPinned":true,"sourceChangeCount":4}]}
        """
        try Data(legacy.utf8).write(to: dir.appendingPathComponent("clipboard.json"))
        let store = ClipboardStore(directory: dir, expiry: .week)
        let loaded = store.load(now: Date(timeIntervalSinceReferenceDate: 60))
        #expect(loaded.map(\.text) == ["ghim cu"])
        #expect(loaded.first?.isPinned == true)
        // A fresh boot can hand out a change count the old file already used.
        #expect(store.record(ClipboardItem(text: "moi", sourceChangeCount: 4)))
        #expect(store.load().map(\.text) == ["moi", "ghim cu"])
    }

    /// The mark is what keeps the toolbar from inviting the same paste again in the
    /// next app, where the keyboard is rebuilt from scratch.
    @Test func pastedMarkSurvivesTheSession() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let store = ClipboardStore(directory: dir)
        #expect(store.record(ClipboardItem(text: "da dan", sourceChangeCount: 7)))
        #expect(store.markPasted(7))
        #expect(ClipboardStore(directory: dir).lastPastedChangeCount() == 7)
    }

    /// Change counts restart at boot, so a mark written before it names a generation
    /// that no longer exists and must not suppress an offer.
    @Test func pastedMarkFromAnotherBootIsNotTrusted() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let stale = """
        {"bootTime":1,"lastCapturedChangeCount":7,"lastPastedChangeCount":7,"items":[]}
        """
        try Data(stale.utf8).write(to: dir.appendingPathComponent("clipboard.json"))
        let store = ClipboardStore(directory: dir)
        #expect(store.lastPastedChangeCount() == nil)
        #expect(store.markPasted(7))
        #expect(store.lastPastedChangeCount() == 7)
    }

    @Test func clearingHistoryDropsThePastedMark() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let store = ClipboardStore(directory: dir)
        #expect(store.record(ClipboardItem(text: "bi mat", sourceChangeCount: 3)))
        #expect(store.markPasted(3))
        store.clear()
        #expect(store.lastPastedChangeCount() == nil)
        #expect(store.load().isEmpty)
    }

    @Test func missingContainerReportsFailure() {
        #expect(!ClipboardStore(directory: nil).record(ClipboardItem(text: "text", sourceChangeCount: 1)))
    }

    private func makeDirectory() throws -> URL {
        let dir = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }
}
