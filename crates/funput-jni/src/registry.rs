//! Safe ID-based ownership for native engine sessions.

use std::collections::HashMap;
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::{Mutex, OnceLock};

use funput_engine::Engine;

static ENGINES: OnceLock<Mutex<HashMap<i64, Engine>>> = OnceLock::new();
static NEXT_HANDLE: AtomicI64 = AtomicI64::new(1);

fn engines() -> &'static Mutex<HashMap<i64, Engine>> {
    ENGINES.get_or_init(|| Mutex::new(HashMap::new()))
}

fn lock_engines() -> std::sync::MutexGuard<'static, HashMap<i64, Engine>> {
    engines()
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner())
}

pub(crate) fn create() -> i64 {
    let handle = NEXT_HANDLE.fetch_add(1, Ordering::Relaxed);
    lock_engines().insert(handle, Engine::new());
    handle
}

pub(crate) fn destroy(handle: i64) {
    lock_engines().remove(&handle);
}

pub(crate) fn with_mut<T>(handle: i64, operation: impl FnOnce(&mut Engine) -> T) -> Option<T> {
    lock_engines().get_mut(&handle).map(operation)
}

#[cfg(test)]
mod tests {
    use super::*;
    use funput_core::InputMethod;

    #[test]
    fn destroyed_and_unknown_handles_are_safe() {
        let handle = create();
        assert!(with_mut(handle, |engine| engine.is_enabled()).is_some());

        destroy(handle);
        destroy(handle);

        assert!(with_mut(handle, |engine| engine.is_enabled()).is_none());
    }

    #[test]
    fn vni_composition_runs_through_registry_handle() {
        let handle = create();
        let buffer = with_mut(handle, |engine| {
            engine.set_method(InputMethod::Vni);
            for key in "an1h".chars() {
                engine.process_char(key);
            }
            engine.buffer().to_owned()
        });

        assert_eq!(buffer.as_deref(), Some("ánh"));
        destroy(handle);
    }

    #[test]
    fn telex_composition_runs_through_registry_handle() {
        let handle = create();
        let buffer = with_mut(handle, |engine| {
            engine.set_method(InputMethod::Telex);
            for key in "anhs".chars() {
                engine.process_char(key);
            }
            engine.buffer().to_owned()
        });

        assert_eq!(buffer.as_deref(), Some("ánh"));
        destroy(handle);
    }
}
