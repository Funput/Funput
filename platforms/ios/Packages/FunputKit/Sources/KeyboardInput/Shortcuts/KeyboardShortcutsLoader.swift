import FunputShared
import os

/// One asynchronous, read-only load per keyboard activation. No work on the key path.
@MainActor
public final class KeyboardShortcutsLoader {
    private let store: any ShortcutsStoring
    private var task: Task<Void, Never>?
    private var generation: UInt64 = 0
    private static let logger = Logger(subsystem: "app.funput.keyboard", category: "Shortcuts")

    public init(store: any ShortcutsStoring = ShortcutsStore.shared) { self.store = store }

    public func activate(receive: @escaping @MainActor (ShortcutLibrary) -> Void) {
        cancel()
        let current = generation
        let store = store
        task = Task { [weak self] in
            do {
                let library = try await store.load()
                guard let self, !Task.isCancelled, self.generation == current else { return }
                receive(library)
            } catch {
                guard let self, !Task.isCancelled, self.generation == current else { return }
                // Do not log descriptions, paths, triggers, or document contents.
                let kind: String
                switch error as? ShortcutsStorageError {
                case .unavailable: kind = "unavailable"
                case .readFailed: kind = "read"
                case .invalidData: kind = "invalid-data"
                case .unsupportedVersion: kind = "unsupported-version"
                default: kind = "unexpected"
                }
                Self.logger.error("Shortcut load failed: \(kind, privacy: .public)")
            }
        }
    }

    public func cancel() {
        generation &+= 1
        task?.cancel()
        task = nil
    }
}
