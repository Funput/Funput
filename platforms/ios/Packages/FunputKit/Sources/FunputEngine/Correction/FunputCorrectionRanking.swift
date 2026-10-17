/// How the engine ranked the candidates it is previewing for the word being typed.
///
/// Mirrors the C ABI's `FunputCorrectionRanking`: `best` and `second` index into
/// `FunputComposer.correctionCandidates()`, and the verdict says what Space would do
/// with them. Ranking counts no metric; only the boundary's own answer does.
public struct FunputCorrectionRanking: Sendable, Equatable {
    /// What the word boundary would do with the ranked candidates.
    public enum Verdict: UInt32, Sendable {
        /// `best` wins by the confidence margin and would be applied.
        case apply = 0
        /// `best` and `second` are too close to call: a pair to offer, not an edit.
        case ambiguous = 1
        /// The word as typed outscores every candidate.
        case asTyped = 2
        /// Nothing to rank, or the word was refused.
        case refused = 3
    }

    public let best: Int?
    public let second: Int?
    public let verdict: Verdict

    public init(best: Int?, second: Int?, verdict: Verdict) {
        self.best = best
        self.second = second
        self.verdict = verdict
    }

    /// What the engine answers when there is nothing to rank.
    public static let refused = FunputCorrectionRanking(best: nil, second: nil, verdict: .refused)
}
