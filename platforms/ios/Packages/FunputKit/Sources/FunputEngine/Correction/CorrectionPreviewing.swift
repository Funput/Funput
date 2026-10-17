/// The engine calls that let the keyboard show a repair *before* the word ends.
///
/// `FunputComposer` already answers the boundary (`chooseCorrection`,
/// `applyCorrection`). These are the three calls that look at the word while it is
/// still being typed: search it, rank what was found, or refuse the repair outright.
/// The composer conforms; the protocol is what lets the keyboard's tests stand a
/// fake in for the search and decide which words dead-end.
@MainActor
public protocol CorrectionPreviewing: AnyObject {
    /// Search the word being typed and park what was found. Returns how many
    /// candidates there are; `correctionCandidates()` then reads them. The
    /// result stays valid until the next letter and is reused by the boundary, so
    /// Space never searches twice.
    func previewCorrection() -> Int

    /// The candidates the last preview (or the parked boundary) found, best touch
    /// score first.
    func correctionCandidates() -> [FunputCorrectionCandidateText]

    /// Rank the parked candidates against `uses` without answering anything.
    func rankCorrection(uses: [UInt32], allowed: [Bool]) -> FunputCorrectionRanking

    /// The user tapped the word as typed: drop the preview, and do not offer the
    /// same repair again when the word ends.
    func refuseCorrection()
}
