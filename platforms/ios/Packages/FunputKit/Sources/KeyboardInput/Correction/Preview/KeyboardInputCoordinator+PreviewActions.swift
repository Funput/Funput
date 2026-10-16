#if os(iOS) && canImport(FunputCore)
import FunputEngine

/// What a tap on one of the bar's correction slots does.
extension KeyboardInputCoordinator {
    /// The user tapped the word as typed: keep it, now and from now on.
    ///
    /// The engine drops its preview so the boundary does not offer the same repair
    /// back; the dictionary remembers the word so it is never offered again.
    @discardableResult
    public func refuseCorrection(
        typed: String, writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        synchronizeBeforeInput(writer)
        guard case let .offered(shown, _) = correctionPreview, shown == typed,
              documentSynchronizer.snapshot?.contextBeforeInput?.hasSuffix(typed) ?? true
        else { return .none }
        correctionPreviewer.refuseCorrection()
        correctionDictionary?.keep(typed)
        correctionDeclines.refused += 1
        correctionPreview = nil
        return .init(suggestionsChanged: true)
    }

    /// The user tapped a repair: commit it, with the space that would have applied it.
    ///
    /// Typed as a space on purpose. The boundary reuses the preview the bar was
    /// drawn from, `chooseCorrection` returns the index the tap forced, and the
    /// repair rides the same transaction a silent one would — so the undo is armed
    /// and the suggestion tracker learns the repaired word, exactly as on Space.
    @discardableResult
    public func acceptCorrection(
        _ text: String, writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        guard case let .offered(_, candidates) = correctionPreview, candidates.contains(text)
        else { return .none }
        forcedCorrection = text
        // Spent by the boundary, or dropped if it never parked anything: it must not
        // outlive this one space.
        defer { forcedCorrection = nil }
        return commit(writer: writer, closesEpoch: true, preservesOneShotShift: true) { builder in
            input(" ", builder: &builder)
        }
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
