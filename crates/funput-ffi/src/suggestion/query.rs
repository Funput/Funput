use funput_suggestions::LearnOutcome;

use crate::abi::safe;

use super::engine::{FunputSuggestionEngine, codepoints_from_raw};
use super::types::FunputSuggestionResult;

/// Record one completed UTF-32 token. Returns false for invalid input or failure.
///
/// # Safety
/// `token` must point to `token_len` readable codepoints, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_learn(
    engine: *mut FunputSuggestionEngine,
    token: *const u32,
    token_len: usize,
) -> bool {
    safe(false, || {
        let Some(engine) = (unsafe { engine.as_mut() }) else {
            return false;
        };
        let Some(codepoints) = codepoints_from_raw(token, token_len) else {
            return false;
        };
        let Some(text) = decode_valid_codepoints(codepoints) else {
            return false;
        };
        !matches!(engine.inner.learn(&text), LearnOutcome::Ignored)
    })
}

/// Record one completed token and that it followed `previous`.
///
/// A null `previous`, or a zero length, means there was no context to vouch for —
/// a sentence boundary, a moved caret, a fresh editor — and behaves exactly like
/// `funput_suggestion_learn`.
///
/// # Safety
/// `previous` and `token` must each point to their stated number of readable
/// codepoints, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_learn_after(
    engine: *mut FunputSuggestionEngine,
    previous: *const u32,
    previous_len: usize,
    token: *const u32,
    token_len: usize,
) -> bool {
    safe(false, || {
        let Some(engine) = (unsafe { engine.as_mut() }) else {
            return false;
        };
        let Some(context) = context_from_raw(previous, previous_len) else {
            return false;
        };
        let Some(codepoints) = codepoints_from_raw(token, token_len) else {
            return false;
        };
        let Some(text) = decode_valid_codepoints(codepoints) else {
            return false;
        };
        let outcome = engine.inner.learn_after(context.as_deref(), &text);
        !matches!(outcome, LearnOutcome::Ignored)
    })
}

/// Return at most three UTF-32 candidates by value. Any failure returns empty.
///
/// # Safety
/// `prefix` must point to `prefix_len` readable codepoints, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_query(
    engine: *const FunputSuggestionEngine,
    prefix: *const u32,
    prefix_len: usize,
) -> FunputSuggestionResult {
    safe(FunputSuggestionResult::default(), || {
        let Some(engine) = (unsafe { engine.as_ref() }) else {
            return FunputSuggestionResult::default();
        };
        let Some(codepoints) = codepoints_from_raw(prefix, prefix_len) else {
            return FunputSuggestionResult::default();
        };
        let Some(text) = decode_valid_codepoints(codepoints) else {
            return FunputSuggestionResult::default();
        };
        FunputSuggestionResult::from_set(engine.inner.suggest(&text))
    })
}

/// Return at most three UTF-32 candidates, with the words that have followed
/// `previous` before moved to the front. Any failure returns empty.
///
/// # Safety
/// `previous` and `prefix` must each point to their stated number of readable
/// codepoints, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_query_with(
    engine: *const FunputSuggestionEngine,
    previous: *const u32,
    previous_len: usize,
    prefix: *const u32,
    prefix_len: usize,
) -> FunputSuggestionResult {
    safe(FunputSuggestionResult::default(), || {
        let Some(engine) = (unsafe { engine.as_ref() }) else {
            return FunputSuggestionResult::default();
        };
        let Some(context) = context_from_raw(previous, previous_len) else {
            return FunputSuggestionResult::default();
        };
        let Some(codepoints) = codepoints_from_raw(prefix, prefix_len) else {
            return FunputSuggestionResult::default();
        };
        let Some(text) = decode_valid_codepoints(codepoints) else {
            return FunputSuggestionResult::default();
        };
        FunputSuggestionResult::from_set(engine.inner.suggest_with(context.as_deref(), &text))
    })
}

/// `None` when the call is malformed; `Some(None)` when the caller had no context
/// to offer, which is a normal thing to say and not an error.
fn context_from_raw(previous: *const u32, len: usize) -> Option<Option<String>> {
    if len == 0 {
        return Some(None);
    }
    decode_valid_codepoints(codepoints_from_raw(previous, len)?).map(Some)
}

fn decode_valid_codepoints(codepoints: &[u32]) -> Option<String> {
    codepoints.iter().copied().map(char::from_u32).collect()
}
