import Foundation
import FunputShared
import KeyboardConfiguration
import Testing

struct KeyboardBootstrapSnapshotStoreTests {
    @Test("Atomic replacement retains the latest complete snapshot")
    func replacement() throws {
        let container = temporaryContainer()
        defer { try? FileManager.default.removeItem(at: container) }
        let store = KeyboardBootstrapSnapshotStore(containerURL: container)
        var first = FunputConfiguration.default
        first.language = .english
        var second = FunputConfiguration.default
        second.heightScale = 0.9

        try store.save(.make(configuration: first, customThemes: []))
        try store.save(.make(configuration: second, customThemes: []))

        #expect(try store.load().configuration == second)
        let directory = container.appendingPathComponent(
            KeyboardBootstrapSnapshotStore.directoryName
        )
        let files = try FileManager.default.contentsOfDirectory(atPath: directory.path)
        #expect(files == [KeyboardBootstrapSnapshotStore.fileName])
    }

    @Test("Repair never replaces a newer valid snapshot")
    func repairPreservesValidSnapshot() throws {
        let container = temporaryContainer()
        defer { try? FileManager.default.removeItem(at: container) }
        let store = KeyboardBootstrapSnapshotStore(containerURL: container)
        var current = FunputConfiguration.default
        current.language = .english
        let currentSnapshot = KeyboardBootstrapSnapshot.make(
            configuration: current,
            customThemes: []
        )

        try store.save(currentSnapshot)
        try store.repairIfNeeded(.make(configuration: .default, customThemes: []))

        #expect(try store.load() == currentSnapshot)
    }

    @Test("Missing and truncated files return failure")
    func invalidData() throws {
        let container = temporaryContainer()
        defer { try? FileManager.default.removeItem(at: container) }
        let store = KeyboardBootstrapSnapshotStore(containerURL: container)
        #expect(throws: (any Error).self) { try store.load() }

        let directory = container
            .appendingPathComponent(KeyboardBootstrapSnapshotStore.directoryName)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let file = directory.appendingPathComponent(KeyboardBootstrapSnapshotStore.fileName)
        try Data("{\"schemaVersion\":".utf8).write(to: file)
        #expect(throws: (any Error).self) { try store.load() }
    }

    /// The keyboard reads on its way to the first frame; only a writer may touch
    /// the directory.
    @Test("Reading a missing snapshot creates nothing on disk")
    func loadCreatesNothing() {
        let container = temporaryContainer()
        let store = KeyboardBootstrapSnapshotStore(containerURL: container)
        #expect(throws: (any Error).self) { try store.load() }
        #expect(!FileManager.default.fileExists(atPath: container.path))
    }

    @Test("Unavailable App Group fails without a partial value")
    func unavailableContainer() {
        let store = KeyboardBootstrapSnapshotStore(containerURL: nil)
        let snapshot = KeyboardBootstrapSnapshot.make(
            configuration: .default,
            customThemes: []
        )
        #expect(throws: (any Error).self) { try store.load() }
        #expect(throws: (any Error).self) { try store.save(snapshot) }
    }

    private func temporaryContainer() -> URL {
        FileManager.default.temporaryDirectory
            .appendingPathComponent("KeyboardBootstrapTests-\(UUID().uuidString)")
    }
}
