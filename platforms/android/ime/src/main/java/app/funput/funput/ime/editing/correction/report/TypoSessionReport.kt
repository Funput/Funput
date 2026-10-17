package app.funput.funput.ime.editing.correction.report

import app.funput.funput.ime.editing.correction.TypoCorrection
import java.util.Locale

/**
 * One log line per keyboard session with what typo correction did, in the same order as the
 * iOS `typo` log, so measurements from both platforms read side by side. Counts only: never
 * a word the user typed.
 *
 * Silent for a session without touches, and the spread resets on every report, so hiding
 * the keyboard twice does not count a session twice. The engine's counters run for its whole
 * lifetime, as on iOS.
 */
internal class TypoSessionReport(
    private val state: TypoCorrection,
    private val log: (String) -> Unit,
) {
    fun report() {
        val spread = state.spread
        if (spread.count == 0) return
        val counts = state.engine.metrics()
        val declines = state.declines
        val line = String.format(
            Locale.ROOT,
            "touches %d sigma %.3f mean %.3f, applied %d reverted %d, offered %d: ambiguous %d " +
                "as-typed %d known %d field %d refused %d, no candidate %d suppressed %d, " +
                "valid near edge %d, candidates %d max %dus",
            spread.count, spread.perAxisSigma, spread.mean, counts.applied, counts.reverted,
            counts.offered, counts.skippedAmbiguous, counts.keptAsTyped, declines.recognized,
            declines.fieldRefused, declines.refused, counts.noCandidate, counts.suppressed,
            counts.validNearEdge, counts.candidatesMax, counts.microsecondsMax,
        )
        spread.reset()
        log(line)
    }
}
