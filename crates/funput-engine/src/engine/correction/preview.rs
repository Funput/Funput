//! Asking ahead of the boundary: preview the repair, rank it, or turn it down.

use crate::Engine;
use crate::correction::score::{self, INVALID_COST};
use crate::correction::state::CorrectionState;
use crate::correction::{self, CorrectionRanking};

impl Engine {
    /// Search the word being typed the way the boundary will, and return how many
    /// candidates it reaches — so a host can show the repair before the space.
    ///
    /// 0 while the word could still become a syllable with one more key, for a word
    /// the gate leaves alone, and for one the user already undid. The candidates are
    /// read through [`Engine::correction_candidates`] and ranked with
    /// [`Engine::rank_correction`]; they stand until the next letter key, and the
    /// boundary that follows takes them as they are rather than searching twice.
    ///
    /// An explicit call, never part of [`Engine::process_key`]: it runs the bounded
    /// replay search, and the keystroke path must not grow by it.
    pub fn preview_correction(&mut self) -> usize {
        correction::preview(&mut self.session)
    }

    /// Rank the candidates in hand — a preview's, or a parked correction's — with
    /// the platform's prior folded in: the best candidate the host allowed, the
    /// runner-up to show beside it when the two are too close to call, and the
    /// verdict. `uses` and `allowed` read as in [`Engine::choose_correction`].
    ///
    /// Counts nothing, so a host may ask as often as it redraws. The ranking is
    /// [`crate::CorrectionVerdict::Refused`] when there is nothing to rank.
    pub fn rank_correction(&self, uses: &[u32], allowed: &[bool]) -> CorrectionRanking {
        self.session
            .correction
            .as_deref()
            .map_or(CorrectionRanking::REFUSED, |state| {
                rank(state, uses, allowed)
            })
    }

    /// The user tapped the word exactly as they typed it: leave it, and do not offer
    /// this spelling again the next time it is typed — the same one-time refusal a
    /// Backspace undo earns. Works on the word being typed or on the correction a
    /// boundary just parked, whichever is in hand, and drops any preview, parked
    /// correction and armed undo with it.
    pub fn refuse_correction(&mut self) {
        correction::refuse(&mut self.session);
    }
}

/// The ranking of whatever candidates the state holds against the word as typed.
///
/// The typed word's own cost is the zero every candidate's cost is measured from,
/// so as a rival it scores exactly what believing a non-word costs — and when it
/// cannot compete at all, nothing.
pub(super) fn rank(state: &CorrectionState, uses: &[u32], allowed: &[bool]) -> CorrectionRanking {
    let as_typed = if state.typed_competes {
        -INVALID_COST
    } else {
        f32::NEG_INFINITY
    };
    score::choose(state.candidates(), uses, allowed, as_typed)
}
