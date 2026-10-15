import Foundation

/// Where the news tab gets its feed; a protocol so tests and previews never touch the network.
protocol NewsFeedFetching {
    func fetch() async throws -> Data
}

/// Reads https://funput.app/blog/feed.json and nothing else.
///
/// An ephemeral session keeps no cookies or shared cache: the request carries no state
/// that could tell one reader from another. Only the app makes it — the keyboard never
/// goes on the network.
struct NewsFeedClient: NewsFeedFetching {
    static let feedURL = URL(string: "https://funput.app/blog/feed.json")!
    private let session: URLSession

    init() {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 15
        configuration.httpAdditionalHeaders = ["Accept": "application/feed+json, application/json"]
        session = URLSession(configuration: configuration)
    }

    func fetch() async throws -> Data {
        let (data, response) = try await session.data(from: Self.feedURL)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
        return data
    }
}

/// The last feed that downloaded, so the tab has something to show offline.
///
/// Kept in the app's Caches directory rather than the App Group: the keyboard has no use
/// for it, and the system may purge it like any other cache.
struct NewsFeedCache {
    private let fileURL: URL

    init(directory: URL = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]) {
        fileURL = directory.appendingPathComponent("news-feed.json")
    }

    func load() -> (data: Data, savedAt: Date)? {
        guard let data = try? Data(contentsOf: fileURL),
              let savedAt = try? fileURL.resourceValues(forKeys: [.contentModificationDateKey])
                  .contentModificationDate
        else { return nil }
        return (data, savedAt)
    }

    func save(_ data: Data) {
        try? data.write(to: fileURL, options: .atomic)
    }
}
