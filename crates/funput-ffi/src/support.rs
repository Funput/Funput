//! Shared helpers for the C ABI boundary: a panic guard, null-safe handle access,
//! and UTF-32 ↔ `char` marshalling — folded into one place so every export stays a
//! thin, well-documented wrapper.

use std::panic::{AssertUnwindSafe, catch_unwind};

use funput_engine::Engine;

use crate::FunputEngine;

/// Run `operation`; if it panics, swallow the unwind and return `default` so the
/// panic never crosses the `extern "C"` boundary into the host (Swift/C++). An
/// engine bug thus drops one keystroke instead of aborting the IME process.
pub(crate) fn safe<T>(default: T, operation: impl FnOnce() -> T) -> T {
    catch_unwind(AssertUnwindSafe(operation)).unwrap_or(default)
}

/// Run `op` against the engine behind a `*mut` handle, guarded by [`safe`]. A null
/// handle — or a panic inside `op` — is a no-op returning `R::default()`.
///
/// # Safety
/// `engine` must come from `funput_engine_new` (and be unfreed), or be null.
pub(crate) unsafe fn with_engine_mut<R: Default>(
    engine: *mut FunputEngine,
    op: impl FnOnce(&mut Engine) -> R,
) -> R {
    safe(R::default(), || {
        unsafe { engine.as_mut() }.map_or_else(R::default, |e| op(&mut e.inner))
    })
}

/// Read-only counterpart of [`with_engine_mut`] for `*const` handles.
///
/// # Safety
/// `engine` must come from `funput_engine_new` (and be unfreed), or be null.
pub(crate) unsafe fn with_engine_ref<R: Default>(
    engine: *const FunputEngine,
    op: impl FnOnce(&Engine) -> R,
) -> R {
    safe(R::default(), || {
        unsafe { engine.as_ref() }.map_or_else(R::default, |e| op(&e.inner))
    })
}

/// Write `chars` into `dst` as UTF-32, up to `dst.len()`; returns the count written.
/// The single truncating copy behind both `FunputResult::from_ime` and
/// `funput_buffer`.
pub(crate) fn copy_codepoints(dst: &mut [u32], chars: impl Iterator<Item = char>) -> usize {
    let mut n = 0;
    for ch in chars {
        if n >= dst.len() {
            break;
        }
        dst[n] = ch as u32;
        n += 1;
    }
    n
}

/// Decode UTF-32 codepoints into a `String`, skipping invalid scalars.
pub(crate) fn decode_codepoints(codepoints: &[u32]) -> String {
    codepoints.iter().filter_map(|&c| char::from_u32(c)).collect()
}
