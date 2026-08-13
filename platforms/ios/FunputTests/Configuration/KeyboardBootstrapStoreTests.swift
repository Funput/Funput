import Foundation
import FunputShared
import KeyboardConfiguration
import KeyboardLayout
import Testing

struct KeyboardBootstrapStoreTests {
    @Test("Snapshot store atomically replaces complete values")
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
        let file = container
            .appendingPathComponent(KeyboardBootstrapSnapshotStore.directoryName)
            .appendingPathComponent(KeyboardBootstrapSnapshotStore.fileName)
        let attributes = try FileManager.default.attributesOfItem(atPath: file.path)
        if let protection = attributes[.protectionKey] as? FileProtectionType {
            #expect(protection == .completeUntilFirstUserAuthentication)
        }
    }

    @Test("Repair preserves a snapshot written by the app")
    func repairPreservesCurrentValue() throws {
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

    @Test("Truncated, future, and unavailable snapshots fail closed")
    func invalidSnapshots() throws {
        let container = temporaryContainer()
        defer { try? FileManager.default.removeItem(at: container) }
        let store = KeyboardBootstrapSnapshotStore(containerURL: container)
        let file = container
            .appendingPathComponent(KeyboardBootstrapSnapshotStore.directoryName)
            .appendingPathComponent(KeyboardBootstrapSnapshotStore.fileName)
        try Data("{".utf8).write(to: file)
        #expect(throws: (any Error).self) { try store.load() }

        let snapshot = KeyboardBootstrapSnapshot.make(
            configuration: .default,
            customThemes: []
        )
        var json = try #require(
            JSONSerialization.jsonObject(with: JSONEncoder().encode(snapshot))
                as? [String: Any]
        )
        json["schemaVersion"] = KeyboardBootstrapSnapshot.currentSchemaVersion + 1
        try JSONSerialization.data(withJSONObject: json).write(to: file)
        #expect(throws: (any Error).self) { try store.load() }

        json["schemaVersion"] = KeyboardBootstrapSnapshot.currentSchemaVersion
        var configuration = try #require(json["configuration"] as? [String: Any])
        configuration["heightScale"] = 5
        json["configuration"] = configuration
        try JSONSerialization.data(withJSONObject: json).write(to: file)
        #expect(throws: (any Error).self) { try store.load() }

        let unavailable = KeyboardBootstrapSnapshotStore(containerURL: nil)
        #expect(throws: (any Error).self) { try unavailable.load() }
    }

    private func temporaryContainer() -> URL {
        FileManager.default.temporaryDirectory
            .appendingPathComponent("KeyboardBootstrapAppTests-\(UUID().uuidString)")
    }
}
