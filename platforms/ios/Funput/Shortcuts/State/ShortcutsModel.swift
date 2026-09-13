import Foundation
import FunputShared
import Combine

@MainActor
final class ShortcutsModel: ObservableObject {
    @Published private(set) var library = ShortcutLibrary()
    @Published private(set) var hasLoaded = false
    @Published private(set) var isLoading = false
    @Published private(set) var isSaving = false
    @Published private(set) var loadError: String?
    @Published var saveError: String?
    @Published var query = ""
    private let store: any ShortcutsStoring

    init(store: any ShortcutsStoring = ShortcutsStore.shared) {
        self.store = store
    }

    var entries: [TextShortcut] { library.entries }
    var canWrite: Bool { hasLoaded && loadError == nil && !isLoading && !isSaving }

    var filteredEntries: [TextShortcut] {
        let search = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !search.isEmpty else { return entries }
        return entries.filter {
            $0.trigger.localizedCaseInsensitiveContains(search)
                || $0.expansion.localizedCaseInsensitiveContains(search)
        }
    }

    func contains(_ entry: TextShortcut) -> Bool { entries.contains { $0.id == entry.id } }
    func isDuplicate(_ entry: TextShortcut) -> Bool { library.isDuplicate(entry) }

    func reload() async {
        guard !isLoading && !isSaving else { return }
        isLoading = true
        defer { isLoading = false }
        do {
            library = try await store.load()
            hasLoaded = true
            loadError = nil
        } catch {
            loadError = error.localizedDescription
        }
    }

    @discardableResult
    func save(_ draft: TextShortcut) async -> Bool {
        guard canWrite, draft.isValid else { return false }
        guard !isDuplicate(draft) else {
            saveError = ShortcutsStorageError.duplicateTrigger.localizedDescription
            return false
        }
        var candidate = library
        let isNew = !contains(draft)
        if let index = candidate.entries.firstIndex(where: { $0.id == draft.id }) {
            candidate.entries[index] = draft
        } else {
            candidate.entries.append(draft)
        }
        guard await commit(candidate) else { return false }
        if isNew { query = "" }
        return true
    }

    @discardableResult
    func delete(_ entry: TextShortcut) async -> Bool {
        guard canWrite else { return false }
        var candidate = library
        candidate.entries.removeAll { $0.id == entry.id }
        return await commit(candidate)
    }

    func update(_ keyPath: WritableKeyPath<ShortcutLibrary, Bool>, to value: Bool) async {
        guard canWrite else { return }
        let previous = library
        var candidate = library
        candidate[keyPath: keyPath] = value
        library = candidate
        if !(await commit(candidate)) { library = previous }
    }

    private func commit(_ candidate: ShortcutLibrary) async -> Bool {
        isSaving = true
        saveError = nil
        defer { isSaving = false }
        do {
            try await store.save(candidate)
            library = candidate
            return true
        } catch {
            saveError = error.localizedDescription
            if let storageError = error as? ShortcutsStorageError,
               [.unavailable, .readFailed, .invalidData, .unsupportedVersion].contains(storageError) {
                loadError = error.localizedDescription
            }
            return false
        }
    }
}
