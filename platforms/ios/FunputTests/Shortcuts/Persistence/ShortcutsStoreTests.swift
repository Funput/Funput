import Foundation
import Testing
@testable import FunputShared

struct ShortcutsStoreTests {
    @Test("Missing files load empty without creating a document")
    func missingFile() async throws {
        let fixture = try ShortcutsTestDirectory()
        defer { fixture.remove() }
        let library = try await ShortcutsStore(directory: fixture.directory).load()
        #expect(library == ShortcutLibrary())
        #expect(!FileManager.default.fileExists(atPath: fixture.file.path))
    }

    @Test("New instances read the exact persisted order, IDs, long Unicode text and options")
    func roundTrip() async throws {
        let fixture = try ShortcutsTestDirectory()
        defer { fixture.remove() }
        let library = ShortcutLibrary(entries: [
            TextShortcut(trigger: "vn", expansion: " việt nam\n👨‍👩‍👧‍👦 "),
            TextShortcut(trigger: "VN", expansion: String(repeating: "Đặng 👋🏽\n", count: 1_000)),
        ], isEnabled: false, smartCase: false, inEnglish: false)
        try await ShortcutsStore(directory: fixture.directory).save(library)
        let loaded = try await ShortcutsStore(directory: fixture.directory).load()
        #expect(loaded == library)
        #expect(loaded.schemaVersion == 1)
        let files = try FileManager.default.contentsOfDirectory(atPath: fixture.directory.path)
        #expect(files == ["shortcuts.json"])
    }

    @Test("Storage rejects empty fields, duplicate IDs and duplicate triggers")
    func invalidLibraries() async throws {
        let fixture = try ShortcutsTestDirectory()
        defer { fixture.remove() }
        let store = ShortcutsStore(directory: fixture.directory)
        let entry = TextShortcut(trigger: "vn", expansion: "việt nam")
        await #expect(throws: ShortcutsStorageError.invalidData) {
            try await store.save(ShortcutLibrary(entries: [TextShortcut(trigger: " \n", expansion: "ok")]))
        }
        await #expect(throws: ShortcutsStorageError.invalidData) {
            try await store.save(ShortcutLibrary(entries: [entry, entry]))
        }
        await #expect(throws: ShortcutsStorageError.duplicateTrigger) {
            try await store.save(ShortcutLibrary(entries: [entry, TextShortcut(trigger: "vn", expansion: "khác")]))
        }
        #expect(!FileManager.default.fileExists(atPath: fixture.file.path))
    }

    @Test("An atomic replacement failure leaves the old file intact")
    func failedWrite() async throws {
        let fixture = try ShortcutsTestDirectory()
        defer { fixture.remove() }
        let original = ShortcutLibrary(entries: [TextShortcut(trigger: "vn", expansion: "việt nam")])
        try await ShortcutsStore(directory: fixture.directory).save(original)
        let bytes = try Data(contentsOf: fixture.file)
        let failing = ShortcutsStore(directory: fixture.directory) { _, _ in
            throw CocoaError(.fileWriteOutOfSpace)
        }
        await #expect(throws: ShortcutsStorageError.writeFailed) {
            try await failing.save(ShortcutLibrary())
        }
        #expect(try Data(contentsOf: fixture.file) == bytes)
        #expect(try await ShortcutsStore(directory: fixture.directory).load() == original)
    }
}
