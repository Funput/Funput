#if os(iOS) && canImport(FunputCore)
import Foundation
import FunputEngine
import os

/// Corrections the keyboard declined on its own side, before the engine ranked
/// anything. Counts only — never a word.
public struct CorrectionDeclines: Sendable, Equatable {
    /// The word on screen is one the dictionary knows: English, a word kept on
    /// purpose, one the user took a correction back on — or the dictionary is still
    /// loading, which answers "known" so that nothing is corrected blind.
    public internal(set) var recognized = 0
    /// The field asked for no autocorrect.
    public internal(set) var fieldRefused = 0

    public init() {}
}

#if DEBUG
extension KeyboardInputCoordinator {
    /// One line per word correction looked at, with the words in it — **Debug builds
    /// only**, for reading why a slip was or was not repaired on a device you are
    /// typing on. Never compiled into a build anyone else runs.
    ///
    /// `log stream --predicate 'subsystem == "app.funput.keyboard" && category == "typo"'`
    func logCorrection(shown: String?, decision: String) {
        let candidates = composer.correctionCandidates()
            .map { "\($0.text) \(String(format: "%.2f", $0.touchScore))/\($0.edits)" }
            .joined(separator: ", ")
        Self.correctionLog.debug(
            "\(shown ?? "?", privacy: .public) → \(decision, privacy: .public) [\(candidates, privacy: .public)]"
        )
    }

    /// Why the engine declined, read off the counters it keeps.
    func declineReason(before: FunputCorrectionCounts) -> String {
        let after = composer.correctionMetrics
        if after.keptAsTyped > before.keptAsTyped { return "kept as typed" }
        if after.skippedAmbiguous > before.skippedAmbiguous { return "too close (Δ)" }
        return "refused"
    }

    private static let correctionLog = Logger(subsystem: "app.funput.keyboard", category: "typo")
}
#endif
#endif
