/// What the host knows about words, asked once per word the engine offers to repair.
///
/// The engine carries no dictionary, so a deliberate English word reaches it looking
/// exactly like a mistyped Vietnamese one — `text ` is a real word and `tẻ` is a real
/// syllable one key away. Only the host can tell those apart.
///
/// Answers have to be synchronous: the word boundary has already deferred an edit and
/// is holding it open, so an answer that arrives later would show the user a flicker.
/// Implementations must therefore be lookups, never I/O.
@MainActor
public protocol CorrectionDictionary: AnyObject {
    /// Whether `word` is one the user has typed before, or one in a shipped word
    /// list. `false` for a word the dictionary has simply never heard of.
    func recognizes(_ word: String) -> Bool

    /// The user took back a correction of `word`, or kept it from the bar: recognize
    /// it from now on, so the same correction is never offered twice.
    func keep(_ word: String)

    /// How likely `word` is, as a prior the engine weighs against touch evidence:
    /// `0` for a word it knows nothing about, larger for a common syllable or one
    /// the user types often. Only the ratio between candidates matters.
    func weight(_ word: String) -> UInt32

    /// The user finished typing `word`: count it, so it weighs more next time.
    func learn(_ word: String)
}

public extension CorrectionDictionary {
    /// A dictionary that only vetoes has no opinion on likelihood.
    func weight(_ word: String) -> UInt32 { 0 }

    func learn(_ word: String) {}
}
