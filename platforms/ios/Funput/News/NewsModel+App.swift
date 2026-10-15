import Foundation

extension NewsModel {
    /// The model the app shell owns. UI tests read a bundled feed instead of the network.
    static func makeForApp() -> NewsModel {
#if DEBUG
        if ProcessInfo.processInfo.environment["FUNPUT_NEWS_TEST_FIXTURE"] != nil {
            let directory = FileManager.default.temporaryDirectory
                .appendingPathComponent(UUID().uuidString, isDirectory: true)
            try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            return NewsModel(client: NewsFixtureClient(), cache: NewsFeedCache(directory: directory))
        }
#endif
        return NewsModel(client: NewsFeedClient(), cache: NewsFeedCache())
    }
}
