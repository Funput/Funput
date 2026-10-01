import Foundation

/// The consonants Vietnamese spelling lacks that a user may still open a syllable
/// with — UniKey's "Cho phép phụ âm đầu Z, F, W, J", one switch per letter.
///
/// The raw values are the engine's own `ONSET_*` wire bits, so the set crosses the
/// FFI as-is (`funput_set_extra_onsets`). Empty — the default — admits none.
struct ExtraOnsetLetters: OptionSet, Hashable {
    let rawValue: UInt8

    static let z = ExtraOnsetLetters(rawValue: UInt8(ONSET_Z))
    static let f = ExtraOnsetLetters(rawValue: UInt8(ONSET_F))
    static let w = ExtraOnsetLetters(rawValue: UInt8(ONSET_W))
    static let j = ExtraOnsetLetters(rawValue: UInt8(ONSET_J))
    static let all: ExtraOnsetLetters = [.z, .f, .w, .j]

    /// A single letter, in the order the settings show them.
    struct Letter: Identifiable, Hashable {
        let member: ExtraOnsetLetters
        /// The letter as typed (`z`).
        let symbol: String
        /// What it lets the user write (`zô, zui`).
        let examples: String

        var id: UInt8 { member.rawValue }
    }

    static let letters: [Letter] = [
        Letter(member: .z, symbol: "z", examples: "zô, zui"),
        Letter(member: .f, symbol: "f", examples: "fải, fan"),
        Letter(member: .w, symbol: "w", examples: "wá, wê"),
        Letter(member: .j, symbol: "j", examples: "jờ, Cư Jút"),
    ]

    /// The letters spelled out (`"zf"`) — the config file's portable form.
    var configValue: String {
        Self.letters.filter { contains($0.member) }.map(\.symbol).joined()
    }

    /// Read a config value; letters it does not know are ignored.
    init(configValue: String) {
        let typed = Set(configValue.lowercased().map(String.init))
        self = Self.letters.reduce(into: []) { set, letter in
            if typed.contains(letter.symbol) { set.insert(letter.member) }
        }
    }

    init(rawValue: UInt8) {
        self.rawValue = rawValue
    }
}
