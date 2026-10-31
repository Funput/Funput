//! The lookup with the words before as context, and reading a context off the ABI.

use crate::abi::safe;

use super::super::engine::{FunputSuggestionEngine, codepoints_from_raw};
use super::super::types::FunputSuggestionResult;
use super::decode_valid_codepoints;

/// As `funput_suggestion_query_with`, with the words before as one UTF-32 string
/// separated by spaces, oldest first: `cảm ơn` reads the two words before. The
/// engine reads as many of the last words as it has contexts for. Null or empty
/// means no context. Any failure returns empty.
///
/// # Safety
/// `context` and `prefix` must each point to their stated number of readable
/// codepoints, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_query_in(
    engine: *const FunputSuggestionEngine,
    context: *const u32,
    context_len: usize,
    prefix: *const u32,
    prefix_len: usize,
) -> FunputSuggestionResult {
    safe(FunputSuggestionResult::default(), || {
        let Some(engine) = (unsafe { engine.as_ref() }) else {
            return FunputSuggestionResult::default();
        };
        let Some(context) = context_from_raw(context, context_len) else {
            return FunputSuggestionResult::default();
        };
        let Some(codepoints) = codepoints_from_raw(prefix, prefix_len) else {
            return FunputSuggestionResult::default();
        };
        let Some(text) = decode_valid_codepoints(codepoints) else {
            return FunputSuggestionResult::default();
        };
        let words = context.as_deref().unwrap_or_default();
        FunputSuggestionResult::from_set(engine.inner.suggest_after_words(words, &text))
    })
}

/// `None` when the call is malformed; `Some(None)` when the caller had no context
/// to offer, which is a normal thing to say and not an error.
pub(super) fn context_from_raw(previous: *const u32, len: usize) -> Option<Option<String>> {
    if len == 0 {
        return Some(None);
    }
    decode_valid_codepoints(codepoints_from_raw(previous, len)?).map(Some)
}
