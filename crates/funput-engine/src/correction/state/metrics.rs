//! What typo correction did, counted for the platforms to report.
//!
//! Counts only — never a word, never a keystroke. The one number that reads back on
//! the design is the revert rate: users undoing corrections means the confidence
//! margin is set too low.

/// Counters kept while typo correction is on, read through
/// [`crate::Engine::correction_metrics`].
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct CorrectionMetrics {
    /// Corrections the platform applied.
    pub applied: u32,
    /// Corrections the user undid with the next Backspace.
    pub reverted: u32,
    /// Words where candidates were found but the top two were too close to call.
    pub skipped_ambiguous: u32,
    /// Words where no candidate beat the word as typed: the finger sat on the keys it
    /// hit, so the engine believed it.
    pub kept_as_typed: u32,
    /// The most candidates any one word produced.
    pub candidates_max: u32,
    /// The longest a candidate search took, in microseconds.
    pub microseconds_max: u32,
}

impl CorrectionMetrics {
    pub(crate) fn note_search(&mut self, candidates: usize, microseconds: u128) {
        self.candidates_max = self.candidates_max.max(candidates as u32);
        let microseconds = u32::try_from(microseconds).unwrap_or(u32::MAX);
        self.microseconds_max = self.microseconds_max.max(microseconds);
    }
}
