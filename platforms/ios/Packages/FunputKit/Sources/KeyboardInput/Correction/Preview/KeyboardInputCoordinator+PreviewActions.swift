#if os(iOS) && canImport(FunputCore)
import FunputEngine

/// What a tap on one of the bar's correction slots does.
extension KeyboardInputCoordinator {
    /// The user tapped the word as typed: keep it, now and from now on.
    ///
    /// The engine drops its preview, or the correction it parked, so the boundary
    /// does not offer the same repair back; the dictionary remembers the word so it
    /// is never offered again.
    @discardableResult
    public func refuseCorrection(
        typed: String, writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        synchronizeBeforeInput(writer)
        guard let offer = correctionPreview?.offer, offer.typed == typed else { return .none }
        if case .offered = correctionPreview,
           !(documentSynchronizer.snapshot?.contextBeforeInput?.hasSuffix(typed) ?? true) {
            return .none
        }
        correctionPreviewer.refuseCorrection()
        correctionDictionary?.keep(typed)
        correctionDeclines.refused += 1
        parkedCorrection = nil
        correctionPreview = nil
        return .init(suggestionsChanged: true)
    }

    /// The user tapped a repair: commit it, with the space that would have applied it.
    ///
    /// Before the word ends it is typed as a space on purpose: the boundary reuses
    /// the preview the bar was drawn from, `chooseCorrection` returns the index the
    /// tap forced, and the repair rides the same transaction a silent one would, so
    /// the undo is armed and the suggestion tracker learns the repaired word exactly
    /// as on Space. After the word ended, the parked correction is answered directly.
    @discardableResult
    public func acceptCorrection(
        _ text: String, writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        switch correctionPreview {
        case let .offered(_, candidates) where candidates.contains(text):
            forcedCorrection = text
            // Spent by the boundary, or dropped if it never parked anything: it
            // must not outlive this one space.
            defer { forcedCorrection = nil }
            return commit(writer: writer, closesEpoch: true, preservesOneShotShift: true) { builder in
                input(" ", builder: &builder)
            }
        case let .parked(typed, candidates) where candidates.contains(text):
            return replaceParkedWord(typed, with: text, writer: writer)
        default:
            return .none
        }
    }

    /// Answer the parked correction from the bar: the committed word and its space
    /// give way to the repair and the same space, and the undo arms as on Space.
    private func replaceParkedWord(
        _ typed: String, with text: String, writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        synchronizeBeforeInput(writer)
        parkedCorrection = nil
        // Read before answering: the answer consumes the count.
        let scalars = composer.pendingCorrectionBackspace
        guard let index = composer.correctionCandidates().firstIndex(where: { $0.text == text }),
              let count = KeyboardReplacement.deletionCount(
                scalars: scalars, context: documentSynchronizer.snapshot?.contextBeforeInput
              ), count > 0
        else {
            // The document is not where the engine left it: let the correction go
            // rather than delete a guess.
            _ = composer.applyCorrection(nil)
            refreshCorrectionPreview()
            return .init(suggestionsChanged: true)
        }
        return commit(writer: writer, closesEpoch: true, preservesOneShotShift: true) { builder in
            let result = composer.applyCorrection(index)
            guard result.action != .none else { return }
            builder.deleteBackward(count: count)
            builder.insert(result.text)
            appliedCorrection = AppliedCorrection(shown: typed, composed: typed)
            refreshCorrectionPreview()
        }
    }

    /// Let a parked correction go before the document moves under it. Backspace with
    /// nothing composing never reaches the engine, so the engine would otherwise keep
    /// the correction parked over a word that is no longer where it left it.
    func dropParkedCorrection() {
        guard parkedCorrection != nil else { return }
        parkedCorrection = nil
        _ = composer.applyCorrection(nil)
    }

    /// The user tapped the undo chip: the same undo Backspace would make.
    @discardableResult
    public func undoCorrectionFromBar(
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        guard case .applied = correctionPreview else { return .none }
        return commit(writer: writer, preservesOneShotShift: true) { builder in
            _ = undoCorrection(builder: &builder)
            refreshCorrectionPreview()
        }
    }
}
#endif
