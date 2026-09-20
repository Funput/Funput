//! The platform's half of typo correction.
//!
//! Two steps, because the engine has no dictionary. A word boundary parks the
//! candidates its touch data can reach and returns what it always returned; the host
//! then ranks them against its own word store and calls [`Engine::apply_correction`],
//! which is the only call here that edits the document. A host that stops after the
//! first step loses nothing — the next keystroke settles the parked state.
//!
//! See `docs/features/typo-correction.md` and [`crate::correction`].

use crate::correction::score::{is_ambiguous, word_prior};
use crate::correction::state::CorrectionState;
use crate::correction::{self, CorrectionCandidate, KeyTouch};
use crate::{Engine, ImeResult};

impl Engine {
    /// Report where the finger landed for the key about to be sent.
    ///
    /// Consumed by the next [`Engine::process_key`], whatever key that turns out to
    /// be, so a touch never attaches itself to a later keystroke. A word with a key
    /// that arrived without one is left alone entirely: half-described evidence would
    /// be worse than none.
    pub fn set_next_key_touch(&mut self, touch: KeyTouch) {
        correction::set_next(&mut self.session, touch);
    }

    /// Whether the last keystroke ended a word on a correction the platform still
    /// owes an answer for.
    ///
    /// A query rather than a field on [`ImeResult`]: that struct's C layout is fixed
    /// and shipped, and hosts built against the current header must keep working.
    pub fn has_pending_correction(&self) -> bool {
        self.correction_state()
            .is_some_and(|state| state.pending.is_some())
    }

    /// The words the parked correction can reach, best touch score first. Empty when
    /// nothing is pending.
    pub fn correction_candidates(&self) -> &[CorrectionCandidate] {
        self.correction_state()
            .map_or(&[], CorrectionState::candidates)
    }

    /// How many characters applying a candidate will delete — the word as it is
    /// displayed, plus the boundary character the platform has already echoed. Lets a
    /// host size its edit before it commits to making one.
    pub fn pending_correction_backspace(&self) -> usize {
        self.correction_state()
            .and_then(|state| state.pending.as_ref())
            .map_or(0, |pending| pending.shown_chars + 1)
    }

    /// Rank the parked candidates with the platform's use counts folded in, and name
    /// the winner — or `None` when the top two are too close to call, which is a
    /// suggestion to offer rather than an edit to make.
    ///
    /// `uses` and `allowed` are both parallel to [`Engine::correction_candidates`].
    /// A short `uses` reads its missing entries as zero, so a host with no word store
    /// can pass `&[]` and still get the touch-only ranking. A short `allowed` reads
    /// its missing entries as permitted, so `&[]` means "consider them all".
    ///
    /// `allowed` is how a host refuses to correct *into* a word its dictionary does
    /// not know. It belongs here rather than in a caller's own filter because the
    /// index this returns points into the unfiltered list, and because the confidence
    /// margin has to be measured against the runner-up that was actually eligible.
    pub fn choose_correction(&self, uses: &[u32], allowed: &[bool]) -> Option<usize> {
        let mut best: Option<(usize, f32)> = None;
        let mut runner_up = f32::NEG_INFINITY;
        for (i, candidate) in self.correction_candidates().iter().enumerate() {
            if !allowed.get(i).copied().unwrap_or(true) {
                continue;
            }
            let score = candidate.touch_score() + word_prior(uses.get(i).copied().unwrap_or(0));
            match best {
                Some((_, leader)) if leader >= score => runner_up = runner_up.max(score),
                Some((_, leader)) => {
                    runner_up = runner_up.max(leader);
                    best = Some((i, score));
                }
                None => best = Some((i, score)),
            }
        }
        let (index, top) = best?;
        (!is_ambiguous(top, runner_up)).then_some(index)
    }

    /// Answer the parked correction: apply the candidate at `index`, or pass `None`
    /// to decline it.
    ///
    /// Declining is not a no-op in every case — it is the word boundary finishing the
    /// job it deferred, which for a non-Vietnamese word is the English restore. An
    /// index that names no candidate declines too, and with nothing pending the call
    /// is a no-op, so a host can always ask.
    pub fn apply_correction(&mut self, index: Option<usize>) -> ImeResult {
        correction::apply(&mut self.session, index)
    }

    /// Whether Backspace would undo a correction rather than delete a character.
    /// A host that passes Backspace straight through must check this first.
    pub fn has_correction_undo(&self) -> bool {
        self.session.buffer.is_empty()
            && self
                .correction_state()
                .is_some_and(|state| state.last.is_some())
    }

    /// The word the user actually typed, while undoing the correction is still one
    /// Backspace away — what a "↩ dduwowfnh" chip shows.
    pub fn correction_undo_text(&self) -> Option<&str> {
        self.correction_state()?
            .last
            .as_ref()
            .map(|applied| applied.shown.as_str())
    }

    fn correction_state(&self) -> Option<&CorrectionState> {
        self.session.correction.as_deref()
    }
}
