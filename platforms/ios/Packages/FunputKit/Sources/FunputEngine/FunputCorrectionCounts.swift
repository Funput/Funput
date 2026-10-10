/// What typo correction has done this session.
///
/// Counts only — never a word, never a keystroke. Each one reads back on a knob: a
/// high `reverted` share means the confidence margin is set too low, a high
/// `skippedAmbiguous` share that it is set too high, a high `keptAsTyped` share that
/// the typed word is trusted too much, and `validNearEdge` bounds how many slips
/// landed on another real word — the ones only sentence context could repair.
public struct FunputCorrectionCounts: Sendable, Equatable {
    public let applied: Int
    public let reverted: Int
    public let skippedAmbiguous: Int
    public let candidatesMax: Int
    public let microsecondsMax: Int
    public let keptAsTyped: Int
    public let offered: Int
    public let noCandidate: Int
    public let validNearEdge: Int

    public init(
        applied: Int = 0,
        reverted: Int = 0,
        skippedAmbiguous: Int = 0,
        candidatesMax: Int = 0,
        microsecondsMax: Int = 0,
        keptAsTyped: Int = 0,
        offered: Int = 0,
        noCandidate: Int = 0,
        validNearEdge: Int = 0
    ) {
        self.applied = applied
        self.reverted = reverted
        self.skippedAmbiguous = skippedAmbiguous
        self.candidatesMax = candidatesMax
        self.microsecondsMax = microsecondsMax
        self.keptAsTyped = keptAsTyped
        self.offered = offered
        self.noCandidate = noCandidate
        self.validNearEdge = validNearEdge
    }
}
