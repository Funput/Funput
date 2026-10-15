import Foundation
import Testing
@testable import Funput

@MainActor
@Suite("News feed")
struct NewsTests {
    private let feed = Data("""
    {"version":"https://jsonfeed.org/version/1.1","items":[
    {"id":"old","url":"https://funput.app/blog/old/","title":"Cũ","date_published":"2026-01-01T00:00:00Z"},
    {"id":"new","url":"https://funput.app/blog/new/","title":"Mới","summary":"Tóm tắt",
     "date_published":"2026-10-01T00:00:00.000Z","tags":["ios","huong-dan"]}
    ]}
    """.utf8)

    @Test("Decodes the website's feed, newest first")
    func decodes() throws {
        let articles = try NewsFeed.decode(feed)
        #expect(articles.map(\.id) == ["new", "old"])
        #expect(articles[0].isGuide)
        #expect(articles[0].platform == "iOS")
        #expect(articles[1].summary.isEmpty)
        #expect(articles[1].tags.isEmpty)
    }

    @Test("A fresh cache is shown without touching the network")
    func freshCacheSkipsNetwork() async throws {
        let cache = try makeCache()
        cache.save(feed)
        let client = StubClient(result: .failure(URLError(.notConnectedToInternet)))
        let model = NewsModel(client: client, cache: cache)
        await model.loadIfNeeded()
        #expect(client.calls == 0)
        #expect(model.articles.count == 2)
        #expect(model.phase == .loaded)
    }

    @Test("A stale cache stays on screen when the download fails")
    func staleCacheSurvivesFailure() async throws {
        let cache = try makeCache()
        cache.save(feed)
        let client = StubClient(result: .failure(URLError(.notConnectedToInternet)))
        let later = Date().addingTimeInterval(NewsModel.freshness + 60)
        let model = NewsModel(client: client, cache: cache, now: { later })
        await model.loadIfNeeded()
        #expect(client.calls == 1)
        #expect(model.articles.count == 2)
        #expect(model.phase == .failed)
    }

    @Test("A download is cached for the next launch")
    func downloadIsCached() async throws {
        let cache = try makeCache()
        let model = NewsModel(client: StubClient(result: .success(feed)), cache: cache)
        await model.loadIfNeeded()
        #expect(model.phase == .loaded)
        #expect(model.articles.count == 2)
        #expect(cache.load()?.data == feed)
    }

    @Test("Pinned guides are distinct pages on the docs site")
    func pinnedGuides() {
        let guides = NewsGuide.pinned
        #expect(Set(guides.map(\.id)).count == guides.count)
        #expect(Set(guides.map(\.url)).count == guides.count)
        #expect(guides.allSatisfy { $0.url.scheme == "https" && $0.url.host == "docs.funput.app" })
    }

    private func makeCache() throws -> NewsFeedCache {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return NewsFeedCache(directory: directory)
    }
}

@MainActor
private final class StubClient: NewsFeedFetching {
    let result: Result<Data, Error>
    private(set) var calls = 0

    init(result: Result<Data, Error>) { self.result = result }

    func fetch() async throws -> Data {
        calls += 1
        return try result.get()
    }
}
