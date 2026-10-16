#if os(iOS) && canImport(FunputCore)
import FunputEngine
import KeyboardLayout
import Testing

/// The preview calls against the real engine: what the bar sees before Space.
@MainActor
struct FunputCorrectionPreviewTests {
    /// A composer with correction on, after typing `đường` in Telex with the final
    /// `g` slipped onto `h` — a word that has dead-ended with one repair in reach.
    private func slipped() -> FunputComposer {
        let composer = FunputComposer()
        composer.configure(
            FunputCompositionOptions(
                inputMethod: .telex, toneStyle: .traditional,
                smartRestore: true, eagerRestore: true, spellCheck: true
            )
        )
        composer.setTypoCorrection(true)
        for (index, scalar) in "dduwowfnh".unicodeScalars.enumerated() {
            composer.setNextKeyTouch(
                index == 8
                    ? KeyboardTouchEvidence(
                        typed: scalar, typedDistance: 0.35, first: .init(scalar: "g", distance: 0.15)
                    )
                    : KeyboardTouchEvidence(typed: scalar, typedDistance: 0.35)
            )
            composer.process(scalar)
        }
        return composer
    }

    @Test("A dead-ended word previews the repair the boundary would make")
    func previews() {
        let composer = slipped()

        #expect(composer.previewCorrection() == 1)
        #expect(composer.correctionCandidates().map(\.text) == ["đường"])
        let ranking = composer.rankCorrection(uses: [0], allowed: [true])
        #expect(ranking.best == 0)
        #expect(ranking.second == nil)
        #expect(ranking.verdict == .apply)
    }

    @Test("A word still being typed previews nothing")
    func liveWord() {
        let composer = FunputComposer()
        composer.setInputMethod(.telex)
        composer.setTypoCorrection(true)
        for scalar in "chaof".unicodeScalars { composer.process(scalar) }

        #expect(composer.previewCorrection() == 0)
        #expect(composer.rankCorrection(uses: [], allowed: []).verdict == .refused)
    }

    @Test("Refusing drops the preview and the boundary parks nothing")
    func refuses() {
        let composer = slipped()
        #expect(composer.previewCorrection() == 1)

        composer.refuseCorrection()

        #expect(composer.previewCorrection() == 0)
        composer.process(" ")
        #expect(!composer.hasPendingCorrection)
        #expect(composer.correctionMetrics.suppressed == 1)
    }

    @Test("Ranking counts no metric; the boundary still does")
    func rankingIsFree() {
        let composer = slipped()
        _ = composer.previewCorrection()
        let before = composer.correctionMetrics

        _ = composer.rankCorrection(uses: [0], allowed: [true])
        _ = composer.rankCorrection(uses: [0], allowed: [true])

        #expect(composer.correctionMetrics == before)
    }
}
#endif
