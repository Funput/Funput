import Foundation
import Testing
@testable import FunputShared

struct ShortcutsStoreProtectionTests {
    @Test("Shortcuts allow backup and retain file protection after repeated atomic writes")
    func fileAttributes() async throws {
        let fixture = try ShortcutsTestDirectory()
        defer { fixture.remove() }
        let store = ShortcutsStore(directory: fixture.directory)
        for enabled in [false, true] {
            try await store.save(ShortcutLibrary(isEnabled: enabled))
            let values = try fixture.directory.resourceValues(forKeys: [.isExcludedFromBackupKey])
            #expect(values.isExcludedFromBackup == false)
            let attributes = try FileManager.default.attributesOfItem(atPath: fixture.file.path)
            assertProtection(attributes)
        }
    }

    @Test("Existing App Group directory callers still exclude private history from backup")
    func existingBackupPolicy() throws {
        let fixture = try ShortcutsTestDirectory()
        defer { fixture.remove() }
        let directory = try #require(AppGroupDirectory.prepare(named: "Clipboard", in: fixture.root))
        let values = try directory.resourceValues(forKeys: [.isExcludedFromBackupKey])
        #expect(values.isExcludedFromBackup == true)
        let attributes = try FileManager.default.attributesOfItem(atPath: directory.path)
        assertProtection(attributes)
    }

    private func assertProtection(_ attributes: [FileAttributeKey: Any]) {
        let value = attributes[.protectionKey]
#if targetEnvironment(simulator)
        // Simulator filesystems may not expose data-protection metadata; devices must.
        if value == nil { return }
#endif
        let protection = (value as? FileProtectionType)?.rawValue ?? value as? String
        #expect(protection == FileProtectionType.completeUntilFirstUserAuthentication.rawValue)
    }
}

struct ShortcutsTestDirectory {
    let root: URL
    var directory: URL { root.appendingPathComponent("Shortcuts", isDirectory: true) }
    var file: URL { directory.appendingPathComponent("shortcuts.json") }

    init() throws {
        root = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: root, withIntermediateDirectories: true)
    }

    func remove() { try? FileManager.default.removeItem(at: root) }
}
