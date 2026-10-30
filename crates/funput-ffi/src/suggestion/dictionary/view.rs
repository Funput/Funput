//! The calls for the process that does not own the store: the app reads it, and
//! posts edits for the keyboard to apply.

use std::ptr;

use funput_suggestions::{Edit, SuggestionConfig, SuggestionEngine, post_edits};

use super::types::edit_kind;
use crate::abi::safe;
use crate::suggestion::engine::{FunputSuggestionEngine, codepoints_from_raw, path_from_raw};
use crate::suggestion::query::decode_valid_codepoints;

/// Open the store at a UTF-8 path to read, while another process may own it.
/// Writes nothing, includes the edits still waiting in the inbox, and keeps no
/// change made through the handle: post those with `funput_suggestion_post_edit`.
/// Failure returns null.
///
/// # Safety
/// `path` must point to `path_len` readable bytes, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_engine_open_snapshot(
    path: *const u8,
    path_len: usize,
) -> *mut FunputSuggestionEngine {
    safe(ptr::null_mut(), || {
        let Some(path) = path_from_raw(path, path_len) else {
            return ptr::null_mut();
        };
        let Ok(inner) = SuggestionEngine::open_snapshot(path, SuggestionConfig::default()) else {
            return ptr::null_mut();
        };
        Box::into_raw(Box::new(FunputSuggestionEngine { inner }))
    })
}

/// Post one edit (`FUNPUT_DICTIONARY_EDIT_*`) for the owner of the store at a
/// UTF-8 path to apply. Needs no handle and writes one new file, so it is safe
/// while the keyboard has the store open. Returns false for invalid input or a
/// failed write. The word is checked when applied, not here.
///
/// # Safety
/// `path` must point to `path_len` readable bytes, or be null. `word` must point to
/// `word_len` readable codepoints, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_post_edit(
    path: *const u8,
    path_len: usize,
    kind: u32,
    word: *const u32,
    word_len: usize,
) -> bool {
    safe(false, || {
        let (Some(path), Some(kind)) = (path_from_raw(path, path_len), edit_kind(kind)) else {
            return false;
        };
        let Some(text) = codepoints_from_raw(word, word_len).and_then(decode_valid_codepoints)
        else {
            return false;
        };
        post_edits(path, &[Edit::new(kind, &text)]).is_ok()
    })
}
