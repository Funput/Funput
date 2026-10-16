#if os(iOS) && canImport(FunputCore)
import FunputEngine

extension KeyboardInputCoordinator {
    /// Recompute what the bar shows about the word under the caret.
    ///
    /// Called after every key that can change the answer. Cheapest checks first:
    /// the field and the dictionary, then the engine's own undo flag, then the
    /// buffer, and the dictionary's veto before the engine is asked to search at
    /// all — a deliberate English word never costs a replay.
    func refreshCorrectionPreview() {
        correctionPreview = computeCorrectionPreview()
    }

    private func computeCorrectionPreview() -> CorrectionPreview? {
        guard usesEngine, allowsAutocorrect, state.usesVietnameseComposition,
              let dictionary = correctionDictionary
        else { return nil }
        if composer.hasCorrectionUndo {
            return appliedCorrection.map { .applied(undoText: $0.shown) }
        }
        let shown = composer.buffer()
        guard !shown.isEmpty, !dictionary.recognizes(shown) else { return nil }
        let signpost = KeyboardInputSignposts.begin("CorrectionPreview")
        defer { KeyboardInputSignposts.end("CorrectionPreview", signpost) }
        guard correctionPreviewer.previewCorrection() > 0 else { return nil }
        let candidates = correctionPreviewer.correctionCandidates()
        let ranking = correctionPreviewer.rankCorrection(
            uses: candidates.map { dictionary.weight($0.text) },
            allowed: Array(repeating: true, count: candidates.count)
        )
        guard ranking.verdict != .refused,
              let best = ranking.best, candidates.indices.contains(best)
        else { return nil }
        var texts = [candidates[best].text]
        // Too close to call: show both and let the user pick, as Space will not.
        if ranking.verdict == .ambiguous,
           let second = ranking.second, candidates.indices.contains(second) {
            texts.append(candidates[second].text)
        }
        return .offered(typed: shown, candidates: texts)
    }
}
#endif
