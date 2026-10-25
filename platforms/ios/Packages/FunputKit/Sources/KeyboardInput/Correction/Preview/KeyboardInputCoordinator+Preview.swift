#if os(iOS) && canImport(FunputCore)
import FunputEngine

extension KeyboardInputCoordinator {
    /// Recompute what the bar shows about the word under the caret.
    ///
    /// Called after every key that can change the answer. Cheapest checks first:
    /// the field and the dictionary, then the engine's own undo flag, then a parked
    /// correction, then the buffer, and the dictionary's veto before the engine is
    /// asked to search at all — a deliberate English word never costs a replay.
    func refreshCorrectionPreview() {
        correctionPreview = computeCorrectionPreview()
    }

    private func computeCorrectionPreview() -> CorrectionPreview? {
        // Before the guards below: undoing an expansion needs neither autocorrect nor a
        // dictionary, and works in English mode. It cannot coexist with a correction,
        // which is born at a different boundary, so no ranking is needed between them.
        if let undo = shortcuts.undo { return .shortcut(undoText: undo.shown) }
        guard usesEngine, allowsAutocorrect, state.usesVietnameseComposition,
              let dictionary = correctionDictionary
        else { return nil }
        if composer.hasCorrectionUndo {
            return appliedCorrection.map { .applied(undoText: $0.shown) }
        }
        if let parked = parkedCorrection {
            // The engine lets a parked correction lapse on the next key; the bar
            // follows it rather than keeping a word that is no longer in reach.
            guard composer.hasPendingCorrection else {
                parkedCorrection = nil
                return nil
            }
            return .parked(typed: parked.shown, candidates: parked.candidates)
        }
        let shown = composer.buffer()
        guard !shown.isEmpty, !dictionary.recognizes(shown) else { return nil }
        let signpost = KeyboardInputSignposts.begin("CorrectionPreview")
        defer { KeyboardInputSignposts.end("CorrectionPreview", signpost) }
        guard correctionPreviewer.previewCorrection() > 0 else { return nil }
        let candidates = correctionPreviewer.correctionCandidates()
        let uses = candidates.map { dictionary.weight($0.text) }
        return rankedTexts(candidates: candidates, uses: uses)
            .map { .offered(typed: shown, candidates: $0) }
    }

    /// The best candidate, then the runner-up when the two are too close to call —
    /// the words the bar shows. `nil` when nothing is left to rank.
    func rankedTexts(candidates: [FunputCorrectionCandidateText], uses: [UInt32]) -> [String]? {
        let ranking = correctionPreviewer.rankCorrection(
            uses: uses, allowed: Array(repeating: true, count: candidates.count)
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
        return texts
    }
}
#endif
