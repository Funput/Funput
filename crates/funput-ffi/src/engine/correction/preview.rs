//! Asking before the word ends: preview the repair, rank it, or turn it down.
//!
//! The three calls a suggestion bar runs while a word is still being typed, so the
//! user sees the correction coming instead of having it land silently on the space.

use funput_engine::{CorrectionRanking, CorrectionVerdict};

use super::FunputEngine;
use crate::abi;

/// `FunputCorrectionRanking.verdict`: the best candidate is clear enough to apply.
pub const CORRECTION_VERDICT_APPLY: u32 = 0;
/// `FunputCorrectionRanking.verdict`: the two best are too close to call — show
/// both, apply neither.
pub const CORRECTION_VERDICT_AMBIGUOUS: u32 = 1;
/// `FunputCorrectionRanking.verdict`: no candidate beat the word as typed.
pub const CORRECTION_VERDICT_AS_TYPED: u32 = 2;
/// `FunputCorrectionRanking.verdict`: nothing the host allowed was left to rank, or
/// there was nothing to rank at all.
pub const CORRECTION_VERDICT_REFUSED: u32 = 3;

/// How the candidates rank once the host's prior is folded in.
///
/// `best` and `second` index [`funput_engine_correction_candidates`]; `-1` means
/// none. `second` is the other word to show when `verdict` is
/// `CORRECTION_VERDICT_AMBIGUOUS`.
///
/// [`funput_engine_correction_candidates`]: super::funput_engine_correction_candidates
#[repr(C)]
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct FunputCorrectionRanking {
    pub best: i32,
    pub second: i32,
    pub verdict: u32,
}

impl FunputCorrectionRanking {
    /// What a null handle, or a panic, answers: nothing to apply.
    pub(crate) const NONE: Self = Self {
        best: -1,
        second: -1,
        verdict: CORRECTION_VERDICT_REFUSED,
    };

    fn from_ranking(ranking: CorrectionRanking) -> Self {
        let index = |slot: Option<usize>| slot.and_then(|i| i32::try_from(i).ok()).unwrap_or(-1);
        Self {
            best: index(ranking.best),
            second: index(ranking.second),
            verdict: match ranking.verdict {
                CorrectionVerdict::Apply => CORRECTION_VERDICT_APPLY,
                CorrectionVerdict::Ambiguous => CORRECTION_VERDICT_AMBIGUOUS,
                CorrectionVerdict::AsTyped => CORRECTION_VERDICT_AS_TYPED,
                CorrectionVerdict::Refused => CORRECTION_VERDICT_REFUSED,
            },
        }
    }
}

/// Search the word being typed the way the boundary will, and return how many
/// candidates it reaches — so the bar can show the repair before the space.
///
/// 0 while the word could still become a syllable, for a word correction leaves
/// alone, and for one the user already refused. Read the candidates with
/// `funput_engine_correction_candidates` and rank them with
/// [`funput_engine_rank_correction`]; they stand until the next letter key, and the
/// boundary that follows reuses them rather than searching twice. An explicit call:
/// `funput_process_key` never runs this search on its own.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_preview_correction(engine: *mut FunputEngine) -> usize {
    unsafe { abi::with_engine_mut(engine, funput_engine::Engine::preview_correction) }
}

/// The user tapped the word exactly as they typed it: leave it, and do not offer
/// this spelling again the next time it is typed. Drops any preview, parked
/// correction and armed undo.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_refuse_correction(engine: *mut FunputEngine) {
    unsafe { abi::with_engine_mut(engine, funput_engine::Engine::refuse_correction) }
}

/// Rank the candidates in hand — a preview's, or a parked correction's — with the
/// host's prior folded in. `uses` and `allowed` read exactly as in
/// `funput_engine_choose_correction`; unlike it, this counts nothing, so the bar may
/// ask as often as it redraws.
///
/// # Safety
/// `engine` must be a valid handle or null. `uses` must point to at least `len`
/// readable `u32` values, or be null; `allowed` to at least `len` readable `bool`
/// values, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_rank_correction(
    engine: *const FunputEngine,
    uses: *const u32,
    allowed: *const bool,
    len: usize,
) -> FunputCorrectionRanking {
    // Guarded by hand for the same reason `funput_engine_choose_correction` is: the
    // `Default` of this struct would read as "apply candidate 0".
    abi::safe(FunputCorrectionRanking::NONE, || {
        let Some(engine) = (unsafe { engine.as_ref() }) else {
            return FunputCorrectionRanking::NONE;
        };
        let (uses, allowed) = unsafe { prior_slices(uses, allowed, len) };
        FunputCorrectionRanking::from_ranking(engine.inner.rank_correction(uses, allowed))
    })
}

/// The two nullable prior arrays as slices, a null one reading as empty.
///
/// # Safety
/// Each non-null pointer must be readable for `len` values.
pub(super) unsafe fn prior_slices<'a>(
    uses: *const u32,
    allowed: *const bool,
    len: usize,
) -> (&'a [u32], &'a [bool]) {
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
    (uses, allowed)
}
