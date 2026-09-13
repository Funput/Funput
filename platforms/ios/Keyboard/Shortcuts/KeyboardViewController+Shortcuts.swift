import FunputShared
import KeyboardInput

extension KeyboardViewController {
    func activateShortcuts() {
        inputCoordinator.beginShortcutActivation()
        shortcutsLoader.activate { [weak self] library in
            self?.inputCoordinator.receiveShortcuts(library)
        }
    }
}

/// Resolve test routing for each load, including when iOS reuses the extension process.
actor KeyboardShortcutSource: ShortcutsStoring {
    func load() async throws -> ShortcutLibrary {
#if DEBUG
        if let fixture = ShortcutsUITestSelection.selectedStore() { return try await fixture.load() }
#endif
        return try await ShortcutsStore.shared.load()
    }

    func save(_ library: ShortcutLibrary) async throws {
        throw ShortcutsStorageError.writeFailed
    }
}
