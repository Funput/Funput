//! Changing the dictionary through the handle that owns the store: the keyboard's.

use funput_suggestions::Edit;

use super::types::{FUNPUT_DICTIONARY_INVALID, edit_kind, outcome_code};
use crate::abi::safe;
use crate::suggestion::engine::{FunputSuggestionEngine, codepoints_from_raw};
use crate::suggestion::query::decode_valid_codepoints;

/// Apply one edit (`FUNPUT_DICTIONARY_EDIT_*`) to `word` and return a
/// `FUNPUT_DICTIONARY_*` outcome. Durable on the next `funput_suggestion_flush`.
///
/// # Safety
/// `engine` must be a live suggestion handle or null and may not be used
/// concurrently. `word` must point to `word_len` readable codepoints, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_apply_edit(
    engine: *mut FunputSuggestionEngine,
    kind: u32,
    word: *const u32,
    word_len: usize,
) -> u32 {
    safe(FUNPUT_DICTIONARY_INVALID, || {
        let Some(engine) = (unsafe { engine.as_mut() }) else {
            return FUNPUT_DICTIONARY_INVALID;
        };
        let Some(kind) = edit_kind(kind) else {
            return FUNPUT_DICTIONARY_INVALID;
        };
        let Some(text) = codepoints_from_raw(word, word_len).and_then(decode_valid_codepoints)
        else {
            return FUNPUT_DICTIONARY_INVALID;
        };
        outcome_code(engine.inner.apply_edit(Edit::new(kind, &text)))
    })
}

/// Apply the edits the app posted with `funput_suggestion_post_edit`, make them
/// durable, and remove them from the inbox. Returns how many were read, or -1 when
/// the inbox could not be read or the journal not written; the edits then stay in
/// the inbox for the next call.
///
/// # Safety
/// `engine` must be a live suggestion handle or null and may not be used concurrently.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_apply_pending_edits(
    engine: *mut FunputSuggestionEngine,
) -> i32 {
    safe(-1, || {
        let Some(engine) = (unsafe { engine.as_mut() }) else {
            return -1;
        };
        engine
            .inner
            .apply_pending_edits()
            .map_or(-1, |count| i32::try_from(count).unwrap_or(i32::MAX))
    })
}
