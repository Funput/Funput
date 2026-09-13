#if DEBUG
import Foundation
import FunputShared

/// UI tests share a unique fixture directory with the extension, never the production library.
enum ShortcutsDebugStoreFactory {
    static var isUITesting: Bool {
        ProcessInfo.processInfo.environment["FUNPUT_SHORTCUTS_TEST_DIRECTORY"] != nil
    }

    static func make() -> any ShortcutsStoring {
        let environment = ProcessInfo.processInfo.environment
        if environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1" { return ShortcutsPreviewStore() }
        guard let name = environment["FUNPUT_SHORTCUTS_TEST_DIRECTORY"] else { return ShortcutsStore.shared }
        guard UUID(uuidString: name) != nil else { return ShortcutsStore(directory: nil) }
        return ShortcutsUITestStore(
            directory: ShortcutsUITestSelection.directory(id: name),
            seedsSamples: environment["FUNPUT_SHORTCUTS_TEST_SEED"] == "1",
            failure: environment["FUNPUT_SHORTCUTS_TEST_FAILURE"]
        )
    }
}

private actor ShortcutsUITestStore: ShortcutsStoring {
    private let store: ShortcutsStore
    private let directory: URL?
    private let seedsSamples: Bool
    private let failure: String?

    init(directory: URL?, seedsSamples: Bool, failure: String?) {
        self.directory = directory
        self.seedsSamples = seedsSamples
        self.failure = failure
        store = ShortcutsStore(directory: directory)
    }

    func load() async throws -> ShortcutLibrary {
        if failure == "read" { throw ShortcutsStorageError.readFailed }
        if seedsSamples, let directory, !FileManager.default.fileExists(atPath: directory.appendingPathComponent("shortcuts.json").path) {
            try await store.save(ShortcutLibrary(entries: ShortcutsPreviewStore.samples))
        }
        return try await store.load()
    }

    func save(_ library: ShortcutLibrary) async throws {
        if failure == "write" { throw ShortcutsStorageError.writeFailed }
        try await store.save(library)
    }
}
#endif
