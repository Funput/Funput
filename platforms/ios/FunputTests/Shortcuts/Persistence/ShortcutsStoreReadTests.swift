import Foundation
import Testing
@testable import FunputShared

struct ShortcutsStoreReadTests {
    @Test("Corrupt or unsupported files are reported and never overwritten", arguments: [
        ("not json", ShortcutsStorageError.invalidData),
        ("{\"schemaVersion\":99}", ShortcutsStorageError.unsupportedVersion),
        ("{\"schemaVersion\":1}", ShortcutsStorageError.invalidData),
    ])
    func damagedData(input: (String, ShortcutsStorageError)) async throws {
        let fixture = try ShortcutsTestDirectory()
        defer { fixture.remove() }
        try FileManager.default.createDirectory(at: fixture.directory, withIntermediateDirectories: true)
        let bytes = Data(input.0.utf8)
        try bytes.write(to: fixture.file)
        let store = ShortcutsStore(directory: fixture.directory)
        await #expect(throws: input.1) { try await store.load() }
        await #expect(throws: input.1) { try await store.save(ShortcutLibrary()) }
        #expect(try Data(contentsOf: fixture.file) == bytes)
    }

    @Test("Duplicate triggers in an existing document are treated as corrupt data")
    func duplicateDocument() async throws {
        let fixture = try ShortcutsTestDirectory()
        defer { fixture.remove() }
        try FileManager.default.createDirectory(at: fixture.directory, withIntermediateDirectories: true)
        let document = ShortcutLibrary(entries: [
            TextShortcut(trigger: "vn", expansion: "việt nam"),
            TextShortcut(trigger: "vn", expansion: "khác"),
        ])
        let bytes = try JSONEncoder().encode(document)
        try bytes.write(to: fixture.file)
        let store = ShortcutsStore(directory: fixture.directory)
        await #expect(throws: ShortcutsStorageError.invalidData) { try await store.load() }
        await #expect(throws: ShortcutsStorageError.invalidData) { try await store.save(ShortcutLibrary()) }
        #expect(try Data(contentsOf: fixture.file) == bytes)
    }

    @Test("Missing App Group and unreadable paths cannot become an empty editable library")
    func unavailableStorage() async throws {
        let unavailable = ShortcutsStore(directory: nil)
        await #expect(throws: ShortcutsStorageError.unavailable) { try await unavailable.load() }
        await #expect(throws: ShortcutsStorageError.unavailable) { try await unavailable.save(ShortcutLibrary()) }
        let fixture = try ShortcutsTestDirectory()
        defer { fixture.remove() }
        try FileManager.default.createDirectory(at: fixture.file, withIntermediateDirectories: true)
        let store = ShortcutsStore(directory: fixture.directory)
        await #expect(throws: ShortcutsStorageError.readFailed) { try await store.load() }
    }
}
