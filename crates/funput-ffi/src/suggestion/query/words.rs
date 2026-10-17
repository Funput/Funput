//! Asking about one whole word rather than a prefix.
//!
//! Both calls are what typo correction ranks its candidates with: the engine that
//! produced them carries no dictionary, so a deliberate English word reaches it
//! looking exactly like a mistyped Vietnamese one. The host asks these before it
//! answers `funput_engine_apply_correction`.

use super::super::engine::{FunputSuggestionEngine, codepoints_from_raw};
use super::decode_valid_codepoints;
use crate::abi::safe;

/// How many times the user has typed `word`, and 0 for one the store has never
/// seen — the `uses` entry that goes into `funput_engine_choose_correction`.
///
/// Read-only, no I/O, and no allocation. A null handle or malformed text answers 0.
///
/// # Safety
/// `word` must point to `word_len` readable codepoints, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_frequency(
    engine: *const FunputSuggestionEngine,
    word: *const u32,
    word_len: usize,
) -> u32 {
    safe(0, || {
        let Some(engine) = (unsafe { engine.as_ref() }) else {
            return 0;
        };
        let Some(codepoints) = codepoints_from_raw(word, word_len) else {
            return 0;
        };
        decode_valid_codepoints(codepoints).map_or(0, |text| engine.inner.frequency(&text))
    })
}

/// Whether `word` is a word at all: one the user has typed, or one in the attached
/// English list.
///
/// The veto. Without it a host would let correction rewrite `text ` as `tẻ`, since
/// `r` sits beside `t` and the engine has no way to know better.
///
/// # Safety
/// `word` must point to `word_len` readable codepoints, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_is_known_word(
    engine: *const FunputSuggestionEngine,
    word: *const u32,
    word_len: usize,
) -> bool {
    safe(false, || {
        let Some(engine) = (unsafe { engine.as_ref() }) else {
            return false;
        };
        let Some(codepoints) = codepoints_from_raw(word, word_len) else {
            return false;
        };
        decode_valid_codepoints(codepoints).is_some_and(|text| engine.inner.is_known_word(&text))
    })
}
