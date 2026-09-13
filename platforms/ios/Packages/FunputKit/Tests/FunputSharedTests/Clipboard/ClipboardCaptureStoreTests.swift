import Foundation
import FunputShared
import Testing

struct ClipboardCaptureStoreTests {
    @Test func recopyKeepsIdentityAndPinAndMovesToFront() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let store = ClipboardStore(directory: dir)
        let pinned = ClipboardItem(text: "pin", isPinned: true, sourceChangeCount: 1)
        #expect(store.capture(pinned))
        #expect(store.capture(ClipboardItem(text: "other", sourceChangeCount: 2)))
        #expect(store.capture(ClipboardItem(text: "pin", sourceChangeCount: 3)))
        let items = store.load()
        #expect(items.map(\.text) == ["pin", "other"])
        #expect(items.first?.id == pinned.id)
        #expect(items.first?.isPinned == true)
        let bytes = try Data(contentsOf: dir.appendingPathComponent("clipboard.json"))
        #expect(store.capture(ClipboardItem(text: "pin", sourceChangeCount: 3)))
        #expect(try Data(contentsOf: dir.appendingPathComponent("clipboard.json")) == bytes)
    }

    @Test func sameCountWithDifferentTextAfterRestartIsNotSuppressed() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let store = ClipboardStore(directory: dir)
        #expect(store.capture(ClipboardItem(text: "before reboot", sourceChangeCount: 1)))
        #expect(store.capture(ClipboardItem(text: "after reboot", sourceChangeCount: 1)))
        #expect(store.load().count == 2)
    }

    /// History written before automatic capture shipped: the same file, but every
    /// change count in it belongs to a previous boot.
    @Test func historyWrittenByAnOlderBuildStillLoadsAndAcceptsCapture() throws {
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
        #expect(store.capture(ClipboardItem(text: "moi", sourceChangeCount: 4)))
        #expect(store.load().map(\.text) == ["moi", "ghim cu"])
    }

    /// The marks are what keep the next session from re-reading the pasteboard,
    /// which the user sees as a system paste banner in every app.
    @Test func marksSurviveTheSessionAndSpareTheNextOneARead() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let store = ClipboardStore(directory: dir)
        #expect(store.capture(ClipboardItem(text: "da dan", sourceChangeCount: 7)))
        #expect(store.markPasted(7))
        #expect(ClipboardStore(directory: dir).lastCapturedChangeCount() == 7)
        #expect(ClipboardStore(directory: dir).lastPastedChangeCount() == 7)
    }

    /// Change counts restart at boot, so a mark written before it names a generation
    /// that no longer exists: the keyboard has to read once and earn the mark again.
    @Test func marksFromAnotherBootAreNotTrusted() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let stale = """
        {"bootTime":1,"lastCapturedChangeCount":7,"lastPastedChangeCount":7,"items":[]}
        """
        try Data(stale.utf8).write(to: dir.appendingPathComponent("clipboard.json"))
        let store = ClipboardStore(directory: dir)
        #expect(store.lastCapturedChangeCount() == nil)
        #expect(store.lastPastedChangeCount() == nil)

        // Re-reading text the file already holds must still re-stamp the mark,
        // otherwise every session after a reboot reads the pasteboard again.
        #expect(store.capture(ClipboardItem(text: "cu", sourceChangeCount: 7)))
        #expect(store.lastCapturedChangeCount() == 7)
        #expect(store.lastPastedChangeCount() == nil)
    }

    @Test func clearingHistoryDropsBothMarks() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let store = ClipboardStore(directory: dir)
        #expect(store.capture(ClipboardItem(text: "bi mat", sourceChangeCount: 3)))
        #expect(store.markPasted(3))
        store.clear()
        #expect(store.lastPastedChangeCount() == nil)
        #expect(store.lastCapturedChangeCount() == nil)
    }

    @Test func missingContainerReportsFailure() {
        #expect(!ClipboardStore(directory: nil).capture(ClipboardItem(text: "text", sourceChangeCount: 1)))
    }

    private func makeDirectory() throws -> URL {
        let dir = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }
}
