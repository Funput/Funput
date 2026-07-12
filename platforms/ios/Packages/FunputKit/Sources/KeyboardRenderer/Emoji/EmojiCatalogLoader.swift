import Foundation

public extension EmojiCatalog {
    static let empty = EmojiCatalog(version: "", emojis: [])

    static let bundled: EmojiCatalog = {
        guard let url = Bundle.module.url(forResource: "EmojiCatalog", withExtension: "json"),
              let data = try? Data(contentsOf: url)
        else { return .empty }
        return decode(data: data)
    }()

    static func decode(data: Data) -> EmojiCatalog {
        (try? JSONDecoder().decode(EmojiCatalog.self, from: data)) ?? .empty
    }
}
