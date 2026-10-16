//! Typo correction — fixing a neighbouring-key slip when the word ends.
//!
//! The iOS and Android system keyboards quietly repair a mistyped word at its
//! boundary; Funput did not, which is most of the accuracy gap users feel when they
//! type without looking. Design: `docs/features/typo-correction.md`.
//!
//! # How it fits the existing boundary
//!
//! The seam is [`offer`], called from `compose::boundary` after gõ tắt and *before*
//! English restore. It cannot hang off the restore decision: by the time a mistyped
//! word reaches the boundary, the eager restore in `compose::pipeline` has usually
//! already rewritten the buffer to the raw keystrokes, so `should_restore` is false
//! for exactly the words this is for. What identifies them instead is that the buffer
//! is not a complete syllable.
//!
//! # Two steps, because the engine has no dictionary
//!
//! The boundary keystroke only *parks* candidates and does what it always did —
//! English restore included, in the same keystroke. The platform then ranks them
//! against its own word store and calls [`crate::Engine::apply_correction`], which is
//! where an edit — if any — is issued. A host that never answers loses nothing: the
//! document already reads as it would have without the feature, and the next
//! keystroke lets the parked state lapse.
//!
//! A host may also ask ahead of the boundary — [`preview`] runs the same search on
//! the live word once it can no longer be a syllable, so the repair can be shown
//! before the space, and the boundary then takes what the preview found.
//!
//! # Layout
//!
//! - `gate` — which finished words correction may look at.
//! - `preview` — the same search, asked for early, and the refusal of its answer.
//! - `touch` — the public [`KeyTouch`] a host reports, and the per-word log of them.
//! - `evidence` — that log as the generators read it: what each key could have been.
//! - `generate` — the generators, and the bounded replay that validates what they
//!   propose.
//! - `score` — [`CorrectionCandidate`] and the arithmetic that ranks them.
//! - `state` — what is parked between the boundary and the platform's answer.

mod evidence;
mod gate;
mod generate;
mod preview;
pub(crate) mod score;
pub(crate) mod state;
mod touch;

pub use score::{CorrectionCandidate, CorrectionRanking, CorrectionVerdict};
pub use state::CorrectionMetrics;
pub use touch::{KeyTouch, MAX_ALTERNATES};

pub(crate) use preview::{invalidate as drop_preview, preview, refuse};
pub(crate) use state::{CorrectionState, apply, discard, flush_pending, sync, take_undo};
pub(crate) use touch::{StoredTouch, invalidate, note_key, set_next, take_next, verify_alignment};

use crate::compose::boundary::Verdict;
use crate::model::Session;

/// Look for a correction for the word that just ended and park it for the platform.
///
/// Parks, and nothing else: the boundary goes on to do exactly what it did before the
/// feature existed, so a host sees no change until it asks. A word the gate refuses
/// leaves every existing boundary path untouched — which is also all a host sees
/// until it turns the setting on.
pub(crate) fn offer(session: &mut Session, boundary: char, verdict: &Verdict) {
    if !gate::eligible(session, verdict.complete) {
        gate::note_valid_near_edge(session, verdict);
        preview::invalidate(session);
        return;
    }
    // Taken out and put back so the search can hold the scratch session mutably while
    // reading the live one. `Box` means this moves a pointer, not the state.
    let Some(mut state) = session.correction.take() else {
        return;
    };
    if undone_before(&mut state, &session.keys) {
        state.metrics.suppressed += 1;
    } else {
        // A preview of this very word already ran the search, and what it found still
        // stands: the boundary does not pay for it twice.
        if !preview::reusable(&state, &session.keys) {
            searched(&mut state, session);
        }
        if state.len > 0 {
            state.metrics.offered += 1;
            state::park(&mut state, session, boundary, verdict.restore);
        } else {
            state.metrics.no_candidate += 1;
        }
    }
    state.preview = None;
    session.correction = Some(state);
}

/// Run the search for the live word, and record how it went.
fn searched(state: &mut CorrectionState, session: &Session) -> usize {
    let started = std::time::Instant::now();
    let found = generate::find(state, &session.config, &session.keys, &session.buffer);
    state
        .metrics
        .note_search(found, started.elapsed().as_micros());
    found
}

/// Whether correction could act on the word that just ended, and so whether the
/// boundary needs to work out if it is a finished syllable.
///
/// Answered before that check rather than after, because the check allocates: a host
/// that never reports where its touches land must not pay for a feature it is not
/// using.
pub(crate) fn wants_verdict(session: &Session) -> bool {
    session
        .correction
        .as_ref()
        .is_some_and(|state| state.touch.matches(&session.keys))
}

/// Whether the user already undid this exact word. Consumed on the way past: typing
/// it again later gets a fresh chance, which is how the system keyboards behave.
fn undone_before(state: &mut CorrectionState, keys: &str) -> bool {
    if state.suppressed.as_deref() != Some(keys) {
        return false;
    }
    state.suppressed = None;
    true
}

#[cfg(test)]
mod tests;
