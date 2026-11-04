import Foundation
import FunputShared
import Testing

/// The pasted mark lives in its own file so the keyboard can read it on its way up
/// without decoding the history.
struct ClipboardMarksFileTests {
    @Test func markReadNeverTouchesTheHistory() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let store = ClipboardStore(directory: dir)
        #expect(store.markPasted(7))
        try Data("not json".utf8).write(to: history(in: dir))
        #expect(ClipboardStore(directory: dir).lastPastedChangeCount() == 7)
    }

    @Test func markingDoesNotRewriteTheHistory() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        let store = ClipboardStore(directory: dir)
        #expect(store.record(ClipboardItem(text: "đã dán", sourceChangeCount: 3)))
        let before = try Data(contentsOf: history(in: dir))
        #expect(store.markPasted(3))
        #expect(try Data(contentsOf: history(in: dir)) == before)
    }

    /// An older build kept the mark inside the history. The first read moves it out,
    /// and from then on the answer no longer depends on the history at all.
    @Test func legacyMarkMigratesOnce() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        try writeLegacyHistory(lastPasted: 9, in: dir)
        let store = ClipboardStore(directory: dir)
        #expect(store.lastPastedChangeCount() == 9)
        #expect(FileManager.default.fileExists(atPath: marks(in: dir).path))

        try Data("not json".utf8).write(to: history(in: dir))
        #expect(store.lastPastedChangeCount() == 9)
    }

    @Test func historyWriteDropsTheLegacyMark() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        try writeLegacyHistory(lastPasted: 9, in: dir)
        let store = ClipboardStore(directory: dir)
        #expect(store.lastPastedChangeCount() == 9)
        #expect(store.record(ClipboardItem(text: "mới", sourceChangeCount: 10)))

        let written = try String(contentsOf: history(in: dir), encoding: .utf8)
        #expect(!written.contains("lastPastedChangeCount"))
        #expect(!written.contains("bootTime"))
        #expect(store.lastPastedChangeCount() == 9)
    }

    @Test func clearingEmptiesTheMarkWithoutAnotherMigration() throws {
        let dir = try makeDirectory()
        defer { try? FileManager.default.removeItem(at: dir) }
        try writeLegacyHistory(lastPasted: 9, in: dir)
        let store = ClipboardStore(directory: dir)
        store.clear()
        #expect(store.lastPastedChangeCount() == nil)
        #expect(FileManager.default.fileExists(atPath: marks(in: dir).path))
    }

    private func writeLegacyHistory(lastPasted: Int, in dir: URL) throws {
        let boot = try #require(ClipboardBootTime.current)
        let legacy = """
        {"bootTime":\(boot),"lastPastedChangeCount":\(lastPasted),"items":[]}
        """
        try Data(legacy.utf8).write(to: history(in: dir))
    }

    private func history(in dir: URL) -> URL { dir.appendingPathComponent("clipboard.json") }
    private func marks(in dir: URL) -> URL { dir.appendingPathComponent("clipboard-marks.json") }

    private func makeDirectory() throws -> URL {
        let dir = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir
    }
}
