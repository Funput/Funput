//! Independent native ownership for personal suggestion sessions.
//!
//! Each handle owns its own lock. The map is locked only long enough to clone the
//! handle's `Arc`, so a call on one handle never waits behind work on another: the
//! IME's main thread asks a small read-only store whether a word is known while the
//! suggestion worker flushes the personal store on its own thread. A `destroy` that
//! races a call in flight drops the engine once that call is done with it.

use std::collections::HashMap;
use std::path::Path;
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::{Arc, Mutex, MutexGuard, OnceLock};

use funput_suggestions::{SeedLanguage, SuggestionConfig, SuggestionEngine};

type Shared = Arc<Mutex<SuggestionEngine>>;

static ENGINES: OnceLock<Mutex<HashMap<i64, Shared>>> = OnceLock::new();
static NEXT_HANDLE: AtomicI64 = AtomicI64::new(1);

/// A poisoned lock still guards a usable value: a panic is caught at the JNI
/// boundary, and the next call should see whatever state it left.
fn guard<T>(mutex: &Mutex<T>) -> MutexGuard<'_, T> {
    mutex
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner())
}

fn engines() -> MutexGuard<'static, HashMap<i64, Shared>> {
    guard(ENGINES.get_or_init(|| Mutex::new(HashMap::new())))
}

fn insert(engine: SuggestionEngine) -> i64 {
    let handle = NEXT_HANDLE.fetch_add(1, Ordering::Relaxed);
    engines().insert(handle, Arc::new(Mutex::new(engine)));
    handle
}

/// The engine behind `handle`, with the map lock already released.
fn shared(handle: i64) -> Option<Shared> {
    engines().get(&handle).cloned()
}

pub(crate) fn create() -> i64 {
    insert(SuggestionEngine::in_memory(SuggestionConfig::default()))
}

pub(crate) fn open(path: &Path) -> Option<i64> {
    SuggestionEngine::open(path, SuggestionConfig::default())
        .ok()
        .map(insert)
}

/// A read-only view of the store at `path`, for the app while the keyboard owns
/// it: writes nothing, and includes the edits still waiting in the inbox.
pub(crate) fn open_snapshot(path: &Path) -> Option<i64> {
    SuggestionEngine::open_snapshot(path, SuggestionConfig::default())
        .ok()
        .map(insert)
}

pub(crate) fn destroy(handle: i64) {
    engines().remove(&handle);
}

/// Attaches the `en.lex` at `path` to the engine behind `handle`. `false` for an
/// unknown handle or a file that will not attach; the engine then keeps the
/// lexicon it had.
pub(crate) fn attach_lexicon(handle: i64, path: &Path) -> bool {
    with_mut(handle, |engine| engine.attach_lexicon(path).is_ok()).unwrap_or(false)
}

/// Says what the shipped Vietnamese data behind `handle` may say. `false` for an
/// unknown handle.
pub(crate) fn set_seed_language(handle: i64, language: SeedLanguage) -> bool {
    with_mut(handle, |engine| engine.set_seed_language(language)).is_some()
}

pub(crate) fn with<T>(handle: i64, operation: impl FnOnce(&SuggestionEngine) -> T) -> Option<T> {
    let engine = shared(handle)?;
    let result = operation(&guard(&engine));
    Some(result)
}

pub(crate) fn with_mut<T>(
    handle: i64,
    operation: impl FnOnce(&mut SuggestionEngine) -> T,
) -> Option<T> {
    let engine = shared(handle)?;
    let result = operation(&mut guard(&engine));
    Some(result)
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

    #[test]
    fn the_seed_language_turns_shipped_predictions_on_and_off() {
        use funput_core::ToneStyle;

        let handle = create();
        let predicted = || with(handle, |engine| engine.suggest_with(Some("cảm"), "").len());
        assert_eq!(predicted(), Some(0), "off until the host says otherwise");
        assert!(set_seed_language(
            handle,
            SeedLanguage::Vietnamese(ToneStyle::Modern)
        ));
        assert_eq!(predicted(), Some(1));
        assert!(set_seed_language(handle, SeedLanguage::Off));
        assert_eq!(predicted(), Some(0));

        destroy(handle);
        assert!(!set_seed_language(handle, SeedLanguage::Off));
    }

    #[test]
    fn a_busy_handle_does_not_hold_up_another() {
        use std::sync::mpsc;
        use std::thread;
        use std::time::Duration;

        let busy = create();
        let other = create();
        let (entered, wait_entered) = mpsc::channel();
        let (release, wait_release) = mpsc::channel::<()>();
        let worker = thread::spawn(move || {
            with_mut(busy, |_| {
                entered.send(()).unwrap();
                wait_release.recv().unwrap();
            })
        });
        wait_entered.recv().unwrap();
        // With one lock for every handle this call would block until `release`.
        let (done, wait_done) = mpsc::channel();
        let reader = thread::spawn(move || done.send(with(other, |_| ()).is_some()).unwrap());
        let answered = wait_done.recv_timeout(Duration::from_secs(5));
        release.send(()).unwrap();
        worker.join().unwrap();
        reader.join().unwrap();
        assert_eq!(answered, Ok(true));
        destroy(busy);
        destroy(other);
    }

    #[test]
    fn destroying_a_handle_in_use_lets_the_call_finish() {
        let handle = create();
        let finished = with(handle, |_| {
            destroy(handle);
            true
        });
        assert_eq!(finished, Some(true));
        assert!(with(handle, |_| ()).is_none());
    }
}
