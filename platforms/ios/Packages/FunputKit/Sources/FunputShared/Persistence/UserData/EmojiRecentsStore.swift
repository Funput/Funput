import Foundation

public struct EmojiRecent: Codable, Equatable, Sendable {
    public let glyph: String
    public let name: String
    public let category: String

    public init(glyph: String, name: String, category: String) {
        self.glyph = glyph
        self.name = name
        self.category = category
    }
}

public struct EmojiRecentsStore {
    public static let limit = 30
    private let defaults: UserDefaults
    private let key: String

    public init(
        suiteName: String = FunputAppGroup.identifier,
        key: String = FunputAppGroup.emojiRecentsKey
    ) {
        self.init(defaults: UserDefaults(suiteName: suiteName) ?? .standard, key: key)
    }

    public init(defaults: UserDefaults, key: String = FunputAppGroup.emojiRecentsKey) {
        self.defaults = defaults
        self.key = key
    }

    public func load() -> [EmojiRecent] {
        guard let data = defaults.data(forKey: key),
              let items = try? JSONDecoder().decode([EmojiRecent].self, from: data)
        else { return [] }
        return Array(items.prefix(Self.limit))
    }

    @discardableResult
    public func record(_ item: EmojiRecent) -> [EmojiRecent] {
        let updated = [item] + load().filter { $0.glyph != item.glyph }
        let limited = Array(updated.prefix(Self.limit))
        if let data = try? JSONEncoder().encode(limited) {
            defaults.set(data, forKey: key)
        }
        return limited
    }
}
