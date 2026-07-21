//! Independent native ownership for personal suggestion sessions.

use std::collections::HashMap;
use std::path::Path;
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::{Mutex, OnceLock};

use funput_suggestions::{SuggestionConfig, SuggestionEngine};

static ENGINES: OnceLock<Mutex<HashMap<i64, SuggestionEngine>>> = OnceLock::new();
static NEXT_HANDLE: AtomicI64 = AtomicI64::new(1);

fn engines() -> &'static Mutex<HashMap<i64, SuggestionEngine>> {
    ENGINES.get_or_init(|| Mutex::new(HashMap::new()))
}

fn lock() -> std::sync::MutexGuard<'static, HashMap<i64, SuggestionEngine>> {
    engines()
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner())
}

fn insert(engine: SuggestionEngine) -> i64 {
    let handle = NEXT_HANDLE.fetch_add(1, Ordering::Relaxed);
    lock().insert(handle, engine);
    handle
}

pub(crate) fn create() -> i64 {
    insert(SuggestionEngine::in_memory(SuggestionConfig::default()))
}

pub(crate) fn open(path: &Path) -> Option<i64> {
    SuggestionEngine::open(path, SuggestionConfig::default())
        .ok()
        .map(insert)
}

pub(crate) fn destroy(handle: i64) {
    lock().remove(&handle);
}

pub(crate) fn with<T>(handle: i64, operation: impl FnOnce(&SuggestionEngine) -> T) -> Option<T> {
    lock().get(&handle).map(operation)
}

pub(crate) fn with_mut<T>(
    handle: i64,
    operation: impl FnOnce(&mut SuggestionEngine) -> T,
) -> Option<T> {
    lock().get_mut(&handle).map(operation)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn handles_are_isolated_and_destroy_is_idempotent() {
        let first = create();
        let second = create();
        assert_ne!(first, second);
        assert!(with(first, |_| ()).is_some());
        destroy(first);
        destroy(first);
        assert!(with(first, |_| ()).is_none());
        assert!(with(second, |_| ()).is_some());
        destroy(second);
    }
}
