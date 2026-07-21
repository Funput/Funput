use crate::support::safe;

use super::engine::{FunputSuggestionEngine, with_mut};
use super::types::FunputSuggestionStats;

/// Flush pending learned tokens to the journal.
///
/// # Safety
/// `engine` must be a live suggestion handle or null and may not be used concurrently.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_flush(engine: *mut FunputSuggestionEngine) -> bool {
    with_mut(engine, |engine| engine.flush().is_ok())
}

/// Replace the journal with a compact crash-safe snapshot.
///
/// # Safety
/// `engine` must be a live suggestion handle or null and may not be used concurrently.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_compact(engine: *mut FunputSuggestionEngine) -> bool {
    with_mut(engine, |engine| engine.compact().is_ok())
}

/// Remove all learned words from memory and local persistence.
///
/// # Safety
/// `engine` must be a live suggestion handle or null and may not be used concurrently.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_reset(engine: *mut FunputSuggestionEngine) -> bool {
    with_mut(engine, |engine| engine.reset().is_ok())
}

/// Return counters and byte estimates without exposing learned text.
///
/// # Safety
/// `engine` must be a live suggestion handle or null and may not be used concurrently.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_stats(
    engine: *const FunputSuggestionEngine,
) -> FunputSuggestionStats {
    safe(FunputSuggestionStats::default(), || {
        unsafe { engine.as_ref() }
            .map(|engine| engine.inner.stats().into())
            .unwrap_or_default()
    })
}
