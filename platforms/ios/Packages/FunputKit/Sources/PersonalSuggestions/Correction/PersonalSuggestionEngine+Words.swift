#if os(iOS) && canImport(FunputCore)
import FunputCore

public extension PersonalSuggestionEngine {
    /// Whether `word` is a word at all: one the user has typed, or one in the
    /// attached English list.
    ///
    /// Read-only and allocation-free on the Rust side — typo correction asks it once
    /// per word the user finishes, on the keystroke path, so it has to be a lookup.
    func recognizes(_ word: String) -> Bool {
        let scalars = word.unicodeScalars.map(\.value)
        return scalars.withUnsafeBufferPointer {
            funput_suggestion_is_known_word(handle, $0.baseAddress, UInt($0.count))
        }
    }

    /// How common a syllable `word` is in the shipped Vietnamese list, as a weight
    /// the engine's prior is written in; 0 for a word that is not in it.
    ///
    /// **Stub, replaced in a later change.** `funput_suggestion_syllable_weight` is
    /// not in the shipped xcframework yet, so every syllable weighs the same and the
    /// prior rests on the user's own frequencies alone, exactly as it did before.
    static func syllableWeight(_ word: String) -> UInt32 { 0 }

    /// How many times the user has typed `word`; 0 for one the store has never seen.
    func frequency(of word: String) -> UInt32 {
        let scalars = word.unicodeScalars.map(\.value)
        return scalars.withUnsafeBufferPointer {
            funput_suggestion_frequency(handle, $0.baseAddress, UInt($0.count))
        }
    }
}
#endif
