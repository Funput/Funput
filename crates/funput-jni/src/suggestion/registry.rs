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

/// Attaches the `en.lex` at `path` to the engine behind `handle`. `false` for an
/// unknown handle or a file that will not attach; the engine then keeps the
/// lexicon it had.
pub(crate) fn attach_lexicon(handle: i64, path: &Path) -> bool {
    with_mut(handle, |engine| engine.attach_lexicon(path).is_ok()).unwrap_or(false)
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

    #[test]
    fn attaching_a_lexicon_fills_empty_slots_and_a_failure_keeps_it() {
        use std::io::Write;

        let tsv = "what\t0\nwhen\t1\nwhich\t2\nwhale\t3\n";
        let mut lexicon = tempfile::NamedTempFile::new().unwrap();
        let bytes = funput_suggestions::lexicon_build::encode(tsv).unwrap();
        lexicon.write_all(&bytes).unwrap();

        let handle = create();
        assert!(!attach_lexicon(handle, Path::new("/nonexistent/en.lex")));
        assert!(attach_lexicon(handle, lexicon.path()));
        assert!(!attach_lexicon(
            handle,
            &lexicon.path().with_extension("missing")
        ));
        assert_eq!(with(handle, |engine| engine.suggest("wh").len()), Some(3));

        destroy(handle);
        assert!(!attach_lexicon(handle, lexicon.path()));
    }
}
