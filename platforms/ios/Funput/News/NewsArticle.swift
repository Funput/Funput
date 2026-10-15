import Foundation

/// One post of the funput.app blog, as its JSON Feed lists it.
struct NewsArticle: Decodable, Identifiable, Hashable {
    let id: String
    let url: URL
    let title: String
    let summary: String
    let published: Date
    let tags: [String]

    enum CodingKeys: String, CodingKey {
        case id, url, title, summary, tags
        case published = "date_published"
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        url = try container.decode(URL.self, forKey: .url)
        title = try container.decode(String.self, forKey: .title)
        // Both are optional in JSON Feed; a post without them still belongs in the list.
        summary = try container.decodeIfPresent(String.self, forKey: .summary) ?? ""
        tags = try container.decodeIfPresent([String].self, forKey: .tags) ?? []
        published = try container.decode(Date.self, forKey: .published)
    }

    /// Step-by-step posts carry this tag on the website.
    var isGuide: Bool { tags.contains("huong-dan") }

    /// The platform a post is about, from its tags, for the chip beside its date.
    var platform: String? {
        tags.lazy.compactMap { Self.platformNames[$0] }.first
    }

    private static let platformNames = [
        "ios": "iOS", "macos": "macOS", "windows": "Windows", "linux": "Linux", "android": "Android",
    ]
}

/// The feed document: only its items matter to the app.
enum NewsFeed {
    private struct Document: Decodable { let items: [NewsArticle] }

    /// Newest first, whatever order the server sent.
    static func decode(_ data: Data) throws -> [NewsArticle] {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .custom { decoder in
            let text = try decoder.singleValueContainer().decode(String.self)
            // The site writes `Date.toISOString()`, which always carries milliseconds;
            // the plain form is accepted too so a hand-written feed still parses.
            if let date = try? Date.ISO8601FormatStyle(includingFractionalSeconds: true).parse(text) {
                return date
            }
            return try Date.ISO8601FormatStyle().parse(text)
        }
        return try decoder.decode(Document.self, from: data).items.sorted { $0.published > $1.published }
    }
}
