import Foundation

public enum KaomojiCategory: String, CaseIterable, Codable, Hashable, Sendable {
    // Declaration order drives both the section order and the bottom-bar strip.
    case recent
    case happy
    case love
    case sad
    case angry
    case surprised
    case confused
    case action
    case animal
    case greeting

    public var displayName: String {
        switch self {
        case .recent: "Dùng gần đây"
        case .happy: "Vui vẻ"
        case .sad: "Buồn bã"
        case .angry: "Giận dữ"
        case .love: "Yêu thương"
        case .surprised: "Ngạc nhiên"
        case .confused: "Bối rối"
        case .action: "Hành động"
        case .animal: "Động vật"
        case .greeting: "Chào hỏi"
        }
    }
}

/// A single text emoticon. Unlike ``EmojiItem`` the payload is a whole string
/// rather than one glyph, so cells must size themselves from measured text.
public struct KaomojiItem: Codable, Hashable, Sendable {
    public let text: String
    public let name: String
    public let category: KaomojiCategory

    public init(text: String, name: String, category: KaomojiCategory) {
        self.text = text
        self.name = name
        self.category = category
    }
}

public struct KaomojiCatalog: Codable, Sendable {
    public let version: String
    public let items: [KaomojiItem]

    public init(version: String, items: [KaomojiItem]) {
        self.version = version
        self.items = items
    }

    public func items(in category: KaomojiCategory) -> [KaomojiItem] {
        items.filter { $0.category == category }
    }
}
