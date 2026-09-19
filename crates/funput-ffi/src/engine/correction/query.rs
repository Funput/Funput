//! The read-only half of the handshake: what the host reads between typing the
//! boundary key and answering.

use super::FunputEngine;
use super::types::{CORRECTION_CAP, FunputCorrectionCandidate};
use crate::abi;

/// What `funput_engine_choose_correction` answers when there is nothing to apply.
const NO_CHOICE: i32 = -1;

/// Copy up to `cap` candidates into `out`, best touch score first, and return how
/// many were written. Never more than [`CORRECTION_CAP`].
///
/// Null-safe: a null handle or a null `out` writes nothing and returns 0.
///
/// # Safety
/// `engine` must be a valid handle or null. `out` must point to at least `cap`
/// writable [`FunputCorrectionCandidate`] values, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_correction_candidates(
    engine: *const FunputEngine,
    out: *mut FunputCorrectionCandidate,
    cap: usize,
) -> usize {
    unsafe {
        abi::with_engine_ref(engine, |e| {
            if out.is_null() {
                return 0;
            }
            let candidates = e.correction_candidates();
            let len = candidates.len().min(cap).min(CORRECTION_CAP);
            let dst = std::slice::from_raw_parts_mut(out, len);
            for (slot, candidate) in dst.iter_mut().zip(candidates) {
                *slot = FunputCorrectionCandidate::from_candidate(candidate);
            }
            len
        })
    }
}

/// How many characters applying a candidate will delete: the word as the app is
/// displaying it, plus the boundary character the host has already echoed.
///
/// Lets a host size one batch edit before it decides whether to make it. 0 when
/// nothing is pending.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_pending_correction_backspace(
    engine: *const FunputEngine,
) -> usize {
    unsafe { abi::with_engine_ref(engine, funput_engine::Engine::pending_correction_backspace) }
}

/// Rank the parked candidates with the host's use counts folded in and return the
/// winner, or `-1` when the top two are too close to call — a pair to offer on the
/// suggestion bar rather than an edit to make.
///
/// `uses` and `allowed` are both parallel to [`funput_engine_correction_candidates`]
/// and both nullable. A null or short `uses` reads its missing entries as zero, so a
/// host with no word store can pass null and still get the touch-only ranking. A null
/// or short `allowed` reads its missing entries as permitted.
///
/// `allowed` is how a host refuses to correct *into* a word its dictionary does not
/// know. It is answered here rather than by filtering the candidate list, because the
/// returned index points into the unfiltered list and the confidence margin has to be
/// measured against the runner-up that was actually eligible.
///
/// # Safety
/// `engine` must be a valid handle or null. `uses` must point to at least `len`
/// readable `u32` values, or be null; `allowed` to at least `len` readable `bool`
/// values, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_choose_correction(
    engine: *const FunputEngine,
    uses: *const u32,
    allowed: *const bool,
    len: usize,
) -> i32 {
    // Guarded by hand rather than through `with_engine_ref`: its fallback is
    // `Default`, and a zero here would tell the host to apply the first candidate.
    // The sentinel for "do not correct" is -1, so a null handle and a panic must
    // both land on that.
    abi::safe(NO_CHOICE, || {
        let Some(engine) = (unsafe { engine.as_ref() }) else {
            return NO_CHOICE;
        };
        let uses = if uses.is_null() {
            &[][..]
        } else {
            unsafe { std::slice::from_raw_parts(uses, len) }
        };
        let allowed = if allowed.is_null() {
            &[][..]
        } else {
            unsafe { std::slice::from_raw_parts(allowed, len) }
        };
        engine
            .inner
            .choose_correction(uses, allowed)
            .and_then(|index| i32::try_from(index).ok())
            .unwrap_or(NO_CHOICE)
    })
}

/// Copy the word the user actually typed into `out` as UTF-32, while undoing the
/// last correction is still one Backspace away, and return how many codepoints were
/// written. 0 once the undo is gone — which is also when the chip should disappear.
///
/// # Safety
/// `engine` must be a valid handle or null. `out` must point to at least `cap`
/// writable `u32` values, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_correction_undo_text(
    engine: *const FunputEngine,
    out: *mut u32,
    cap: usize,
) -> usize {
    unsafe {
        abi::with_engine_ref(engine, |e| {
            let Some(text) = e.correction_undo_text() else {
                return 0;
            };
            if out.is_null() {
                return 0;
            }
            let dst = std::slice::from_raw_parts_mut(out, cap);
            abi::copy_codepoints(dst, text.chars())
        })
    }
}
