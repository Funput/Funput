//! Typo correction over the C ABI: the handshake a keyboard runs at a word boundary.
//!
//! Two steps, because `funput-engine` carries no dictionary. The host reports where
//! each touch landed, types the key as usual, and then — only when
//! [`funput_engine_has_pending_correction`] says so — reads the candidates, weighs
//! them against its own word store, and answers with
//! [`funput_engine_apply_correction`]. A host that never answers loses nothing: the
//! next keystroke settles whatever the boundary deferred.
//!
//! Nothing here is reachable until `funput_set_typo_correction(engine, true)`.
//!
//! # Layout
//!
//! - this file — the calls that change engine state.
//! - `query` — the read-only calls between the two steps.
//! - `types` — the POD structs both directions carry.

mod query;
mod types;

pub use query::{
    funput_engine_choose_correction, funput_engine_correction_candidates,
    funput_engine_correction_metrics, funput_engine_correction_undo_text,
    funput_engine_pending_correction_backspace,
};
pub use types::{
    CORRECTION_CAP, CORRECTION_CHARS_CAP, FunputCorrectionCandidate, FunputCorrectionMetrics,
    FunputKeyTouch, TOUCH_ALTERNATE_CAP,
};

use super::{FunputEngine, FunputResult};
use crate::abi;

/// Report where the finger landed for the key about to be sent.
///
/// Consumed by the next `funput_process_key`, whatever key that turns out to be, so
/// a touch never attaches itself to a later keystroke. A word with a key that
/// arrived without one is left alone entirely — half-described evidence would be
/// worse than none — so a host either reports every key of a word or none of them.
///
/// Null-safe both ways: a null handle or a null `touch` does nothing.
///
/// # Safety
/// `engine` must be a valid handle or null. `touch` must point to a readable
/// [`FunputKeyTouch`], or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_set_next_key_touch(
    engine: *mut FunputEngine,
    touch: *const FunputKeyTouch,
) {
    unsafe {
        abi::with_engine_mut(engine, |e| {
            if let Some(touch) = touch.as_ref().and_then(FunputKeyTouch::decode) {
                e.set_next_key_touch(touch);
            }
        })
    }
}

/// Whether the last keystroke ended a word on a correction still waiting for an
/// answer.
///
/// A call of its own rather than a field on [`FunputResult`]: that struct crosses the
/// ABI by value and hosts are built separately from the header, so growing it would
/// mismatch silently until every one of them is rebuilt.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_has_pending_correction(engine: *const FunputEngine) -> bool {
    unsafe { abi::with_engine_ref(engine, funput_engine::Engine::has_pending_correction) }
}

/// Answer the parked correction: apply the candidate at `index`, or pass a negative
/// index to decline it.
///
/// Declining is not always a no-op — it is the word boundary finishing the job it
/// deferred, which for a non-Vietnamese word is the English restore. An index past
/// the end declines too, and with nothing pending the call is a no-op, so a host can
/// always ask.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_apply_correction(
    engine: *mut FunputEngine,
    index: i32,
) -> FunputResult {
    let index = usize::try_from(index).ok();
    unsafe {
        abi::with_engine_mut(engine, |e| {
            FunputResult::from_ime(&e.apply_correction(index))
        })
    }
}

/// Whether Backspace would undo the last correction rather than delete a character.
///
/// A host that passes Backspace straight through to the app must ask this first:
/// when it is true, `funput_backspace` returns an `ACTION_SEND` that restores what
/// the user typed, and passing the key through as well would eat one more character.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_has_correction_undo(engine: *const FunputEngine) -> bool {
    unsafe { abi::with_engine_ref(engine, funput_engine::Engine::has_correction_undo) }
}

#[cfg(test)]
mod tests;
