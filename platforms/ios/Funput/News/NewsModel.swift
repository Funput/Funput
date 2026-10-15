import Combine
import Foundation

/// The news tab's state: the cached feed first, then a download when it is old.
@MainActor
final class NewsModel: ObservableObject {
    enum Phase: Equatable {
        case idle
        case loading
        case loaded
        case failed
    }

    /// How long a downloaded feed counts as current. Posts arrive a few times a month,
    /// so opening the tab rarely needs the network at all.
    static let freshness: TimeInterval = 6 * 60 * 60

    @Published private(set) var articles: [NewsArticle] = []
    @Published private(set) var phase = Phase.idle
    @Published private(set) var updatedAt: Date?
    private let client: any NewsFeedFetching
    private let cache: NewsFeedCache
    private let now: () -> Date

    init(client: any NewsFeedFetching, cache: NewsFeedCache, now: @escaping () -> Date = Date.init) {
        self.client = client
        self.cache = cache
        self.now = now
    }

    /// Shows the cached feed straight away, then downloads only if it is stale or missing.
    func loadIfNeeded() async {
        if articles.isEmpty, let cached = cache.load(), let decoded = try? NewsFeed.decode(cached.data) {
            articles = decoded
            updatedAt = cached.savedAt
            phase = .loaded
        }
        if let updatedAt, now().timeIntervalSince(updatedAt) < Self.freshness { return }
        await refresh()
    }

    /// Downloads the feed. On failure the articles already shown stay on screen.
    func refresh() async {
        guard phase != .loading else { return }
        phase = .loading
        do {
            let data = try await client.fetch()
            articles = try NewsFeed.decode(data)
            cache.save(data)
            updatedAt = now()
            phase = .loaded
        } catch {
            phase = .failed
        }
    }
}
