import FunputEngine
import KeyboardInput
import os
import UIKit

extension KeyboardViewController {
    /// Write out what typo correction did, and how steady the user's fingers were.
    ///
    /// Counts only: how many corrections landed, how many were undone, and the spread
    /// of the touches behind them. Never a word and never a key.
    ///
    /// It goes to the log rather than anywhere durable because of what it is for. The
    /// whole feature turns on a number nobody has measured — how far a real finger
    /// lands from a key centre — and the fastest way to learn it is to read it off a
    /// device that has been typed on. `log stream --predicate 'category == "typo"'`.
    func reportCorrectionSession() {
        let spread = inputCoordinator.touchSpread
        guard spread.count > 0 else { return }
        // `viewWillDisappear` arrives more than once for a keyboard extension, and a
        // session counted twice is a session that looks twice as certain as it is.
        inputCoordinator.resetTouchSpread()
        let counts = inputCoordinator.correctionCounts
        // Default level, not `.info`: Console hides info messages unless the reader
        // knows to turn them on, and this line is the whole point of the measurement.
        os_log(
            .default,
            log: Self.correctionLog,
            "touches %{public}d σ %{public}.3f (mean %{public}.3f) · applied %{public}d reverted %{public}d ambiguous %{public}d · candidates %{public}d max %{public}dµs",
            spread.count,
            spread.perAxisSigma,
            spread.mean,
            counts.applied,
            counts.reverted,
            counts.skippedAmbiguous,
            counts.candidatesMax,
            counts.microsecondsMax
        )
    }

    private static let correctionLog = OSLog(subsystem: "app.funput.keyboard", category: "typo")
}
