use std::collections::HashMap;
use std::ptr;
use std::slice;

use crate::abi::safe;

/// Opaque per-app VI/EN memory handle, independent of [`crate::FunputEngine`] —
/// it only decides "should this app be Vietnamese", never touches composition.
/// A single-owner handle: create with [`funput_app_language_new`], release with
/// [`funput_app_language_free`].
pub struct FunputAppLanguage {
    pub(crate) remembered: HashMap<String, bool>,
}

/// Create an empty per-app memory. Release it with [`funput_app_language_free`].
#[unsafe(no_mangle)]
pub extern "C" fn funput_app_language_new() -> *mut FunputAppLanguage {
    safe(ptr::null_mut(), || {
        Box::into_raw(Box::new(FunputAppLanguage {
            remembered: HashMap::new(),
        }))
    })
}

/// Free a handle created by [`funput_app_language_new`]. Null is ignored.
///
/// # Safety
/// `handle` must come from [`funput_app_language_new`] and not be freed already.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_app_language_free(handle: *mut FunputAppLanguage) {
    safe((), || {
        if !handle.is_null() {
            drop(unsafe { Box::from_raw(handle) });
        }
    });
}

/// Decode `len` UTF-8 bytes at `ptr` into a `&str`. A null pointer yields an
/// empty string; invalid UTF-8 yields `None`. App ids (bundle id / exe name /
/// WM_CLASS) are technical identifiers, not user-typed text, so this carries
/// plain UTF-8 rather than the UTF-32 codec the composition API uses.
///
/// # Safety
/// `ptr` must point to at least `len` readable bytes, or be null.
pub(crate) unsafe fn str_from_raw<'a>(ptr: *const u8, len: usize) -> Option<&'a str> {
    if ptr.is_null() {
        return Some("");
    }
    std::str::from_utf8(unsafe { slice::from_raw_parts(ptr, len) }).ok()
}

/// Run `op` against the handle behind a `*mut` pointer, guarded by [`safe`]. A
/// null handle — or a panic inside `op` — is a no-op returning `R::default()`.
///
/// # Safety
/// `handle` must come from [`funput_app_language_new`] (and be unfreed), or be
/// null.
pub(crate) unsafe fn with_mut<R: Default>(
    handle: *mut FunputAppLanguage,
    op: impl FnOnce(&mut FunputAppLanguage) -> R,
) -> R {
    safe(R::default(), || {
        unsafe { handle.as_mut() }.map_or_else(R::default, op)
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn new_and_free_round_trip() {
        let handle = funput_app_language_new();
        assert!(!handle.is_null());
        unsafe { funput_app_language_free(handle) };
    }

    #[test]
    fn free_is_null_safe() {
        unsafe { funput_app_language_free(ptr::null_mut()) };
    }
}
