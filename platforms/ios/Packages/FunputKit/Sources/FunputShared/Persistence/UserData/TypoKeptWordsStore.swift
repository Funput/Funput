import Foundation

/// Words the user took a typo correction back on, kept so the keyboard never makes
/// that correction again.
///
/// They are the user's own words, so they are treated like the personal lexicon:
/// stored only on this device, capped, and dropped when that lexicon is reset —
/// the list is written together with the reset token it belongs to, and a list
/// under any other token reads as empty.
public struct TypoKeptWordsStore {
    public static let limit = 200
    private let defaults: UserDefaults
    private let key: String

    private struct Payload: Codable {
        var resetToken: UUID?
        var words: [String]
    }

    public init(
        suiteName: String = FunputAppGroup.identifier,
        key: String = FunputAppGroup.typoKeptWordsKey
    ) {
        self.init(defaults: UserDefaults(suiteName: suiteName) ?? .standard, key: key)
    }

    public init(defaults: UserDefaults, key: String = FunputAppGroup.typoKeptWordsKey) {
        self.defaults = defaults
        self.key = key
    }

    /// The kept words, oldest first — none if the lexicon was reset since.
    public func load(resetToken: UUID?) -> [String] {
        guard let data = defaults.data(forKey: key),
              let payload = try? JSONDecoder().decode(Payload.self, from: data),
              payload.resetToken == resetToken
        else { return [] }
        return Array(payload.words.suffix(Self.limit))
    }

    public func save(_ words: [String], resetToken: UUID?) {
        let payload = Payload(resetToken: resetToken, words: Array(words.suffix(Self.limit)))
        if let data = try? JSONEncoder().encode(payload) {
            defaults.set(data, forKey: key)
        }
    }
}
