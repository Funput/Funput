import Foundation

public enum EmojiCategory: String, CaseIterable, Codable, Hashable, Sendable {
    case recent
    case smileysPeople = "smileys_people"
    case animalsNature = "animals_nature"
    case foodDrink = "food_drink"
    case activities
    case travelPlaces = "travel_places"
    case objects
    case symbols
    case flags

    public var accessibilityLabel: String {
        switch self {
        case .recent: "Dùng gần đây"
        case .smileysPeople: "Mặt cười và con người"
        case .animalsNature: "Động vật và thiên nhiên"
        case .foodDrink: "Đồ ăn và thức uống"
        case .activities: "Hoạt động"
        case .travelPlaces: "Du lịch và địa điểm"
        case .objects: "Đồ vật"
        case .symbols: "Biểu tượng"
        case .flags: "Cờ"
        }
    }
}

public struct EmojiItem: Codable, Hashable, Sendable {
    public let glyph: String
    public let name: String
    public let category: EmojiCategory

    public init(glyph: String, name: String, category: EmojiCategory) {
        self.glyph = glyph
        self.name = name
        self.category = category
    }
}

public struct EmojiCatalog: Codable, Sendable {
    public let version: String
    public let emojis: [EmojiItem]

    public init(version: String, emojis: [EmojiItem]) {
        self.version = version
        self.emojis = emojis
    }

    public func items(in category: EmojiCategory) -> [EmojiItem] {
        emojis.filter { $0.category == category }
    }
}
