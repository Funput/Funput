import Foundation

public extension KaomojiCatalog {
    static let empty = KaomojiCatalog(version: "", items: [])

    static var bundled: KaomojiCatalog {
        guard let url = Bundle.module.url(forResource: "KaomojiCatalog", withExtension: "json"),
              let data = try? Data(contentsOf: url)
        else { return .empty }
        return decode(data: data)
    }

    static func decode(data: Data) -> KaomojiCatalog {
        (try? JSONDecoder().decode(KaomojiCatalog.self, from: data)) ?? .empty
    }
}
