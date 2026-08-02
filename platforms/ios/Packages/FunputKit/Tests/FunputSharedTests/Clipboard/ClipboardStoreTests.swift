import Foundation
import FunputShared
import Testing

@Suite("Clipboard store")
struct ClipboardStoreTests {
    private let epoch = Date(timeIntervalSince1970: 1_800_000_000)

    @Test("Records newest first and de-duplicates by text")
    func recording() throws {
        let store = try makeStore()
        store.record(item("một", at: epoch), now: epoch)
        store.record(item("hai", at: epoch), now: epoch)
        store.record(item("một", at: epoch), now: epoch)
        #expect(store.load(now: epoch).map(\.text) == ["một", "hai"])
    }

    @Test("Unpinned entries expire, pinned entries survive")
    func expiry() throws {
        let store = try makeStore()
        store.record(item("tạm", at: epoch), now: epoch)
        let pinned = ClipboardItem(
            text: "giữ", capturedAt: epoch, isPinned: true, sourceChangeCount: 2
        )
        store.record(pinned, now: epoch)

        let later = epoch.addingTimeInterval(ClipboardStore.expiry + 1)
        #expect(store.load(now: epoch).count == 2)
        #expect(store.load(now: later).map(\.text) == ["giữ"])
    }

    @Test("The cap evicts the oldest unpinned entry and never a pinned one")
    func capacity() throws {
        let store = try makeStore()
        let oldestPinned = ClipboardItem(
            text: "ghim", capturedAt: epoch, isPinned: true, sourceChangeCount: 0
        )
        store.record(oldestPinned, now: epoch)
        for index in 0...ClipboardStore.limit {
            store.record(item("mục\(index)", at: epoch, changeCount: index + 1), now: epoch)
        }
        let stored = store.load(now: epoch)
        #expect(stored.count == ClipboardStore.limit)
        #expect(stored.contains { $0.text == "ghim" })
        #expect(!stored.contains { $0.text == "mục0" })
    }

    @Test("The captured change count outlives the entry it came from")
    func changeCountSurvivesDeletion() throws {
        let store = try makeStore()
        let entry = item("xin chào", at: epoch, changeCount: 77)
        store.record(entry, now: epoch)
        store.remove(id: entry.id, now: epoch)
        #expect(store.load(now: epoch).isEmpty)
        #expect(store.lastCapturedChangeCount() == 77)
    }

    @Test("Pinning survives a reload")
    func pinning() throws {
        let directory = try makeDirectory()
        let entry = item("ghim tôi", at: epoch)
        ClipboardStore(directory: directory).record(entry, now: epoch)
        ClipboardStore(directory: directory).setPinned(true, id: entry.id, now: epoch)
        #expect(ClipboardStore(directory: directory).load(now: epoch).first?.isPinned == true)
    }

    @Test("clear wipes the entries and the captured change count")
    func clearing() throws {
        let store = try makeStore()
        store.record(item("bí mật", at: epoch, changeCount: 5), now: epoch)
        store.clear()
        #expect(store.load(now: epoch).isEmpty)
        #expect(store.lastCapturedChangeCount() == nil)
    }

    @Test("A missing container degrades to an empty store instead of crashing")
    func withoutContainer() {
        let store = ClipboardStore(directory: nil)
        store.record(item("rơi", at: epoch), now: epoch)
        #expect(store.load(now: epoch).isEmpty)
    }

    /// Copied passwords must not ride out of the device in a backup.
    @Test("The prepared directory is excluded from backups")
    func excludedFromBackup() throws {
        let container = try makeDirectory()
        let directory = try #require(
            AppGroupDirectory.prepare(named: "Clipboard", in: container)
        )
        let values = try directory.resourceValues(forKeys: [.isExcludedFromBackupKey])
        #expect(values.isExcludedFromBackup == true)
    }

    private func item(_ text: String, at date: Date, changeCount: Int = 1) -> ClipboardItem {
        ClipboardItem(text: text, capturedAt: date, sourceChangeCount: changeCount)
    }

    private func makeStore() throws -> ClipboardStore {
        ClipboardStore(directory: try makeDirectory())
    }

    private func makeDirectory() throws -> URL {
        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("clipboard-tests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory
    }
}
