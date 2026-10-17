/// What the suggestion bar shows about typo correction for the word under the caret.
///
/// Set by `KeyboardInputCoordinator.refreshCorrectionPreview()` after every key that
/// can change it, and read by the host when it publishes the bar. Words, not
/// indices: the bar and the engine can disagree about order, but never about text.
public enum CorrectionPreview: Equatable, Sendable {
    /// The word has dead-ended and the engine can reach a repair. `typed` is the
    /// word as the screen shows it; `candidates` is the best repair, then a second
    /// one when the two are too close to call. Space applies the first; tapping
    /// `typed` keeps it.
    case offered(typed: String, candidates: [String])
    /// The word is committed, and the space after it, because the engine would not
    /// pick a repair on its own. The candidates stay on the bar until the next key:
    /// tapping one replaces the word and keeps the space; tapping `typed` keeps it
    /// for good.
    case parked(typed: String, candidates: [String])
    /// A repair was just applied, and one tap takes it back: `undoText` is the word
    /// that would come back.
    case applied(undoText: String)

    /// The word and its repairs, for the two shapes that show them.
    public var offer: (typed: String, candidates: [String])? {
        switch self {
        case let .offered(typed, candidates), let .parked(typed, candidates):
            (typed, candidates)
        case .applied:
            nil
        }
    }
}

/// The correction the boundary last applied, kept while the engine still offers to
/// undo it. `shown` is what the screen showed when Space was pressed and what an
/// undo puts back; `composed` is the syllable the user watched while typing it.
struct AppliedCorrection: Equatable {
    let shown: String
    let composed: String
}

/// A correction the boundary left unanswered because the engine would not choose
/// on its own: what the bar offers over the committed word until the next key
/// flushes it on the engine's side.
struct ParkedCorrection: Equatable {
    let shown: String
    let candidates: [String]
}
