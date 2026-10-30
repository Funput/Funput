import Combine
import Foundation
import PersonalDictionary

/// The dictionary page's state: which list is showing, how it is sorted and searched,
/// and the words the store answered with.
///
/// Owned by the page itself, so every visit reads the store afresh: the keyboard
/// learns and drains edits while the app is away.
@MainActor
final class PersonalDictionaryModel: ObservableObject {
    @Published private(set) var page = DictionaryPage()
    @Published private(set) var hasLoaded = false
    @Published private(set) var isLoading = false
    @Published private(set) var loadError: String?
    /// The last edit that could not be saved, shown once and then cleared.
    @Published var actionError: String?
    @Published var filter: DictionaryFilter = .learned {
        didSet { if filter != oldValue { scheduleReload(after: .zero) } }
    }
    @Published var sort: DictionarySort = .mostUsed {
        didSet { if sort != oldValue { scheduleReload(after: .zero) } }
    }
    @Published var search = "" {
        didSet { if search != oldValue { scheduleReload(after: Self.searchPause) } }
    }

    /// Long enough that a word typed at speed is one query, short enough to feel live.
    static let searchPause = Duration.milliseconds(150)

    let store: any PersonalDictionaryStoring
    private var pendingReload: Task<Void, Never>?

    init(store: any PersonalDictionaryStoring) {
        self.store = store
    }

    var query: DictionaryQuery {
        DictionaryQuery(filter: filter, sort: sort, search: search)
    }

    /// The most any listed word has been used: what the frequency bars measure against.
    var highestUses: Int {
        page.words.map(\.uses).max() ?? 0
    }

    func reload() async {
        let asked = query
        isLoading = true
        defer { isLoading = false }
        do {
            let answer = try await store.load(asked)
            // A newer filter, sort or search has been asked for since; its own
            // load will land, and this one must not overwrite it.
            guard asked == query else { return }
            page = answer
            loadError = nil
        } catch {
            loadError = error.localizedDescription
        }
        hasLoaded = true
    }

    private func scheduleReload(after pause: Duration) {
        pendingReload?.cancel()
        pendingReload = Task { [weak self] in
            if pause > .zero { try? await Task.sleep(for: pause) }
            guard !Task.isCancelled else { return }
            await self?.reload()
        }
    }
}
