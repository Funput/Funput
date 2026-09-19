/// What typo correction has done this session.
///
/// Counts only — never a word, never a keystroke. Two of them read back on the
/// design: a high `reverted` share means the confidence margin is set too low, and a
/// high `skippedAmbiguous` share means it is set too high.
public struct FunputCorrectionCounts: Sendable, Equatable {
    public let applied: Int
    public let reverted: Int
    public let skippedAmbiguous: Int
    public let candidatesMax: Int
    public let microsecondsMax: Int

    public init(
        applied: Int = 0,
        reverted: Int = 0,
        skippedAmbiguous: Int = 0,
        candidatesMax: Int = 0,
        microsecondsMax: Int = 0
    ) {
        self.applied = applied
        self.reverted = reverted
        self.skippedAmbiguous = skippedAmbiguous
        self.candidatesMax = candidatesMax
        self.microsecondsMax = microsecondsMax
    }
}
