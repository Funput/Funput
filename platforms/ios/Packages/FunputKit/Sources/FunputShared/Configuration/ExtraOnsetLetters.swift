/// Initial consonants admitted beyond Vietnamese spelling. App Group storage uses
/// letters ("zj"), matching the desktop configuration's `preferences.extraOnsets`.
public struct ExtraOnsetLetters: OptionSet, Codable, Hashable, Sendable {
    public let rawValue: UInt8

    public init(rawValue: UInt8) { self.rawValue = rawValue }

    public static let z = ExtraOnsetLetters(rawValue: 1 << 0)
    public static let f = ExtraOnsetLetters(rawValue: 1 << 1)
    public static let w = ExtraOnsetLetters(rawValue: 1 << 2)
    public static let j = ExtraOnsetLetters(rawValue: 1 << 3)
    public static let all: ExtraOnsetLetters = [.z, .f, .w, .j]

    /// One selectable letter, with examples shared by settings presentations.
    public struct Letter: Identifiable, Hashable, Sendable {
        public let member: ExtraOnsetLetters
        public let symbol: String
        public let examples: String
        public var id: UInt8 { member.rawValue }
    }

    /// Stable display and serialization order.
    public static let letters: [Letter] = [
        Letter(member: .z, symbol: "z", examples: "zô, zui"),
        Letter(member: .f, symbol: "f", examples: "fải, fan"),
        Letter(member: .w, symbol: "w", examples: "wá, wê"),
        Letter(member: .j, symbol: "j", examples: "jờ, Cư Jút"),
    ]

    public var configValue: String {
        Self.letters.filter { contains($0.member) }.map(\.symbol).joined()
    }

    /// Accepts any ordering and case, ignoring duplicate and unsupported letters.
    public init(configValue: String) {
        self = []
        let symbols = Set(configValue.lowercased().map(String.init))
        for letter in Self.letters where symbols.contains(letter.symbol) {
            insert(letter.member)
        }
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        self.init(configValue: try container.decode(String.self))
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(configValue)
    }
}
