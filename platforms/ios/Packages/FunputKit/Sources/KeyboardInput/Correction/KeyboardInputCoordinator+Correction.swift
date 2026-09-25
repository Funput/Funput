#if os(iOS) && canImport(FunputCore)
import FunputEngine

extension KeyboardInputCoordinator {
    /// What typo correction has done this session. Counts only — never a word.
    public var correctionCounts: FunputCorrectionCounts {
        composer.correctionMetrics
    }

    /// Answer the correction the word boundary just parked, into the same transaction
    /// the boundary key is still building.
    ///
    /// It has to be the same one. A second transaction would close the echo epoch
    /// between the two edits, expose the intermediate document to `textDidChange`,
    /// and let the suggestion tracker learn the typo before the repair replaced it.
    ///
    /// Declining is not a no-op: it is the boundary finishing the English restore it
    /// deferred, so this runs whether or not a correction is wanted.
    func applyPendingCorrection(context: inout String?, builder: inout InputTransactionBuilder) {
        guard usesEngine, composer.hasPendingCorrection else { return }
        let result = composer.applyCorrection(chooseCorrection(context: context))
        guard result.action != .none else { return }
        guard let count = KeyboardReplacement.deletionCount(
            scalars: result.deleteCount, context: context
        ) else {
            abandonUnsafeReplacement()
            return
        }
        builder.deleteBackward(count: count)
        if let current = context { context = String(current.dropLast(count)) }
        builder.insert(result.text)
        context = (context ?? "") + result.text
    }

    /// Which candidate to apply, or `nil` to decline.
    private func chooseCorrection(context: String?) -> Int? {
        let shown = pendingWord(context: context)
        guard allowsAutocorrect else {
            correctionDeclines.fieldRefused += 1
            return nil
        }
        // No dictionary, no corrections. The engine offers any structurally valid
        // syllable the touches can reach, and without something to weigh them
        // against, applying one would be a guess.
        guard let dictionary = correctionDictionary else { return nil }
        // A word that is already a word is what the user meant, however odd it looks
        // to an engine that only knows Vietnamese spelling.
        if let shown, dictionary.recognizes(shown) {
            correctionDeclines.recognized += 1
            #if DEBUG
            logCorrection(shown: shown, decision: "known word")
            #endif
            return nil
        }
        let candidates = composer.correctionCandidates()
        #if DEBUG
        let before = composer.correctionMetrics
        #endif
        // Every candidate is allowed to win. Filtering to words the dictionary knows
        // is measured to cost nine tenths of the repairs until the list is far larger
        // than the one shipped today — see `docs/features/typo-correction.md` §12.1.
        let chosen = composer.chooseCorrection(
            uses: [], allowed: Array(repeating: true, count: candidates.count)
        )
        #if DEBUG
        logCorrection(
            shown: shown,
            decision: chosen.map { "applied \(candidates[$0].text)" }
                ?? declineReason(before: before)
        )
        #endif
        return chosen
    }

    /// The word the app is showing for the correction being answered, read back out of
    /// the document rather than out of the engine: the boundary character sits after
    /// it, and the engine already counts both together.
    private func pendingWord(context: String?) -> String? {
        guard let context else { return nil }
        let scalars = composer.pendingCorrectionBackspace
        guard scalars > 1 else { return nil }
        let view = context.unicodeScalars
        guard let start = view.index(view.endIndex, offsetBy: -scalars, limitedBy: view.startIndex)
        else { return nil }
        let end = view.index(view.endIndex, offsetBy: -1)
        return String(String.UnicodeScalarView(view[start..<end]))
    }
}
#endif
