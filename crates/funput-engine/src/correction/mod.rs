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
//! The boundary keystroke only *parks* candidates and returns what it always did. The
//! platform then ranks them against its own word store and calls
//! [`crate::Engine::apply_correction`], which is where an edit — if any — is issued.
//! A host that never answers loses nothing: the next keystroke flushes the parked
//! state and replays whatever the boundary would have done.
//!
//! # Layout
//!
//! - `touch` — the public [`KeyTouch`] a host reports, and the per-word log of them.
//! - `search` — the bounded replay that turns touch data into candidate words.
//! - `score` — [`CorrectionCandidate`] and the arithmetic that ranks them.
//! - `state` — what is parked between the boundary and the platform's answer.

pub(crate) mod score;
mod search;
pub(crate) mod state;
mod touch;

pub use score::CorrectionCandidate;
pub use state::CorrectionMetrics;
pub use touch::{KeyTouch, MAX_ALTERNATES};

pub(crate) use state::{CorrectionState, apply, discard, flush_pending, sync, take_undo};
pub(crate) use touch::{invalidate, note_key, set_next, take_next, verify_alignment};

use funput_core::{InputMethod, is_bare_shaped_vowel};

use crate::compose::boundary::Verdict;
use crate::model::Session;

/// Look for a correction for the word that just ended and park it for the platform.
///
/// Returns whether one is waiting. `false` leaves every existing boundary path
/// exactly as it was — which is also all a host sees until it turns the setting on.
pub(crate) fn offer(session: &mut Session, boundary: char, verdict: &Verdict) -> bool {
    if !eligible(session, verdict) {
        return false;
    }
    // Taken out and put back so the search can hold the scratch session mutably while
    // reading the live one. `Box` means this moves a pointer, not the state.
    let Some(mut state) = session.correction.take() else {
        return false;
    };
    let found = if undone_before(&mut state, &session.keys) {
        0
    } else {
        let started = std::time::Instant::now();
        let found = search::find(&mut state, &session.config, &session.keys, &session.buffer);
        state
            .metrics
            .note_search(found, started.elapsed().as_micros());
        found
    };
    if found > 0 {
        park(&mut state, session, boundary, verdict.restore);
    }
    session.correction = Some(state);
    found > 0
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

/// Whether the word that just ended may be looked at at all.
///
/// Every clause here answers "is this a slip?", and each one that says no leaves the
/// word to the engine's existing behaviour. They are ordered cheapest first; the
/// syllable checks at the end are the only ones that allocate.
fn eligible(session: &Session, verdict: &Verdict) -> bool {
    // The state is built when the setting goes on and dropped when it goes off, so
    // its absence is the switch.
    let Some(state) = session.correction.as_ref() else {
        return false;
    };
    session.enabled
        // A word the user flipped by hand is the form they asked for.
        && session.restore_override.is_none()
        && !session.buffer.is_empty()
        && !verdict.complete
        && state.touch.matches(&session.keys)
        && !has_literal_digit(session)
        && !is_all_uppercase(&session.buffer)
        // `aw` → `ă` is not a complete syllable, but it is exactly the letter the
        // user asked for.
        && !is_bare_shaped_vowel(&session.buffer)
}

/// A digit in the word means it is not a syllable — except under VNI, where the tone
/// and shape modifiers *are* digits.
fn has_literal_digit(session: &Session) -> bool {
    session.config.method != InputMethod::Vni && session.buffer.chars().any(|c| c.is_ascii_digit())
}

/// `NHSF` is an acronym typed in anger, not a fumbled syllable.
fn is_all_uppercase(text: &str) -> bool {
    text.chars().any(char::is_uppercase) && !text.chars().any(char::is_lowercase)
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

/// Snapshot what the platform will need, before the boundary clears the session.
fn park(state: &mut CorrectionState, session: &Session, boundary: char, restore: bool) {
    let pending = state.pending.get_or_insert_with(state::Pending::default);
    pending.shown.clear();
    pending.shown.push_str(&session.buffer);
    pending.shown_chars = session.buffer.chars().count();
    pending.keys.clear();
    pending.keys.push_str(&session.keys);
    pending.boundary = boundary;
    pending.restore = restore;
}

#[cfg(test)]
mod tests;
