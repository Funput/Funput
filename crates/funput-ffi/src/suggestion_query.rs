use funput_suggestions::LearnOutcome;

use crate::suggestion_types::FunputSuggestionResult;
use crate::suggestions::{FunputSuggestionEngine, codepoints_from_raw};
use crate::support::safe;

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

fn decode_valid_codepoints(codepoints: &[u32]) -> Option<String> {
    codepoints.iter().copied().map(char::from_u32).collect()
}
