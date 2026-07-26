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
    public let localizedName: String?
    public let searchTerms: [String]
    public let category: EmojiCategory

    public init(
        glyph: String,
        name: String,
        localizedName: String? = nil,
        searchTerms: [String] = [],
        category: EmojiCategory
    ) {
        self.glyph = glyph
        self.name = name
        self.localizedName = localizedName
        self.searchTerms = searchTerms
        self.category = category
    }

    private enum CodingKeys: String, CodingKey {
        case glyph, name, localizedName, searchTerms, category
    }

    public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        glyph = try values.decode(String.self, forKey: .glyph)
        name = try values.decode(String.self, forKey: .name)
        localizedName = try values.decodeIfPresent(String.self, forKey: .localizedName)
        searchTerms = try values.decodeIfPresent([String].self, forKey: .searchTerms) ?? []
        category = try values.decode(EmojiCategory.self, forKey: .category)
    }
}

public struct EmojiCatalog: Codable, Sendable {
    public let version: String
    public let annotationVersion: String
    public let emojis: [EmojiItem]

    public init(version: String, annotationVersion: String = "", emojis: [EmojiItem]) {
        self.version = version
        self.annotationVersion = annotationVersion
        self.emojis = emojis
    }

    public func items(in category: EmojiCategory) -> [EmojiItem] {
        emojis.filter { $0.category == category }
    }

    private enum CodingKeys: String, CodingKey {
        case version, annotationVersion, emojis
    }

    public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        version = try values.decode(String.self, forKey: .version)
        annotationVersion = try values.decodeIfPresent(String.self, forKey: .annotationVersion) ?? ""
        emojis = try values.decode([EmojiItem].self, forKey: .emojis)
    }
}
