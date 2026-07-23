use std::path::Path;
use std::ptr;
use std::slice;

use funput_suggestions::{SuggestionConfig, SuggestionEngine};

use crate::abi::safe;

/// Opaque, single-owner personal suggestion handle. It is independent from the
/// Vietnamese composition engine and must be driven from a serial worker.
pub struct FunputSuggestionEngine {
    pub(crate) inner: SuggestionEngine,
}

#[unsafe(no_mangle)]
pub extern "C" fn funput_suggestion_engine_new_in_memory() -> *mut FunputSuggestionEngine {
    safe(ptr::null_mut(), || {
        Box::into_raw(Box::new(FunputSuggestionEngine {
            inner: SuggestionEngine::in_memory(SuggestionConfig::default()),
        }))
    })
}

/// Open a local store from a UTF-8 path. Failure returns null without logging.
///
/// # Safety
/// `path` must point to `path_len` readable bytes, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_engine_open(
    path: *const u8,
    path_len: usize,
) -> *mut FunputSuggestionEngine {
    safe(ptr::null_mut(), || {
        let Some(bytes) = bytes_from_raw(path, path_len) else {
            return ptr::null_mut();
        };
        let Ok(path) = std::str::from_utf8(bytes) else {
            return ptr::null_mut();
        };
        let Ok(inner) = SuggestionEngine::open(Path::new(path), SuggestionConfig::default()) else {
            return ptr::null_mut();
        };
        Box::into_raw(Box::new(FunputSuggestionEngine { inner }))
    })
}

/// # Safety
/// `engine` must be a live suggestion handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_suggestion_engine_free(engine: *mut FunputSuggestionEngine) {
    safe((), || {
        if !engine.is_null() {
            drop(unsafe { Box::from_raw(engine) });
        }
    });
}

pub(crate) fn bytes_from_raw<'a>(pointer: *const u8, len: usize) -> Option<&'a [u8]> {
    if pointer.is_null() {
        return (len == 0).then_some(&[]);
    }
    Some(unsafe { slice::from_raw_parts(pointer, len) })
}

pub(crate) fn codepoints_from_raw<'a>(pointer: *const u32, len: usize) -> Option<&'a [u32]> {
    if pointer.is_null() {
        return (len == 0).then_some(&[]);
    }
    Some(unsafe { slice::from_raw_parts(pointer, len) })
}

pub(crate) fn with_mut(
    engine: *mut FunputSuggestionEngine,
    operation: impl FnOnce(&mut SuggestionEngine) -> bool,
) -> bool {
    safe(false, || {
        unsafe { engine.as_mut() }
            .map(|engine| operation(&mut engine.inner))
            .unwrap_or(false)
    })
}
