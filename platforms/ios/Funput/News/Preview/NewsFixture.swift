#if DEBUG
import Foundation
import SwiftUI

/// A small feed in the website's exact shape, for previews and UI tests.
struct NewsFixtureClient: NewsFeedFetching {
    func fetch() async throws -> Data { Data(NewsFixtureClient.json.utf8) }

    static let json = """
    {"version":"https://jsonfeed.org/version/1.1","title":"Funput Blog","items":[
    {"id":"cach-bat-ban-phim-funput-iphone-ipad","url":"https://funput.app/blog/cach-bat-ban-phim-funput-iphone-ipad/",
     "title":"Cách bật bàn phím Funput trên iPhone và iPad","summary":"Thêm Funput vào danh sách bàn phím và bật truy cập đầy đủ.",
     "date_published":"2026-10-01T00:00:00.000Z","tags":["ios","huong-dan"]},
    {"id":"telex-vni-va-telex-nang-cao","url":"https://funput.app/blog/telex-vni-va-telex-nang-cao/",
     "title":"Telex, VNI và Telex nâng cao","summary":"Ba kiểu gõ khác nhau ở đâu và nên chọn kiểu nào.",
     "date_published":"2026-09-20T00:00:00.000Z","tags":["kieu-go"]}
    ]}
    """
}

#Preview("Tin tức · Light") {
    let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
    NavigationStack {
        NewsScreen(model: NewsModel(client: NewsFixtureClient(), cache: NewsFeedCache(directory: directory)))
    }
    .preferredColorScheme(.light)
}

#Preview("Tin tức · Dark") {
    let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
    NavigationStack {
        NewsScreen(model: NewsModel(client: NewsFixtureClient(), cache: NewsFeedCache(directory: directory)))
    }
    .preferredColorScheme(.dark)
}
#endif
