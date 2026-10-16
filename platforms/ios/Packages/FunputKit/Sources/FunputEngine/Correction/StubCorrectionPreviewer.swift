#if os(iOS) && canImport(FunputCore)
/// **Stub, replaced in a later change.** The engine calls behind
/// ``CorrectionPreviewing`` (`funput_engine_preview_correction`,
/// `funput_engine_rank_correction`, `funput_engine_refuse_correction`) are not in
/// the shipped xcframework yet, so until they are this answers the way an engine
/// with nothing to preview would: no candidates, nothing to rank, and a refusal that
/// has nothing to drop. With it wired in the keyboard behaves exactly as it did
/// before the suggestion bar learned about corrections.
///
/// Once the ABI lands, `FunputComposer` conforms to ``CorrectionPreviewing`` itself
/// and this type goes away.
@MainActor
public final class StubCorrectionPreviewer: CorrectionPreviewing {
    private let composer: FunputComposer

    public init(composer: FunputComposer) {
        self.composer = composer
    }

    public func previewCorrection() -> Int { 0 }

    /// The boundary's parked candidates are real today, so a forced accept can still
    /// find the word the bar named among them.
    public func correctionCandidates() -> [FunputCorrectionCandidateText] {
        composer.correctionCandidates()
    }

    public func rankCorrection(uses: [UInt32], allowed: [Bool]) -> FunputCorrectionRanking {
        .refused
    }

    public func refuseCorrection() {}
}
#endif
