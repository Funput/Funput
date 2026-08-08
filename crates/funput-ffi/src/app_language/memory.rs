use super::handle::{FunputAppLanguage, str_from_raw, with_mut};

/// Load one remembered `(id, enabled)` pair — the host calls this once per row
/// read from its own settings file at startup, mirroring
/// [`crate::funput_add_shortcut`]. An empty or invalid `id` is ignored.
///
/// # Safety
/// `id` must point to at least `id_len` readable bytes, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_app_language_seed(
    handle: *mut FunputAppLanguage,
    id: *const u8,
    id_len: usize,
    enabled: bool,
) -> bool {
    unsafe {
        with_mut(handle, |state| {
            let Some(id) = str_from_raw(id, id_len) else {
                return false;
            };
            if id.is_empty() {
                return false;
            }
            state.remembered.insert(id.to_string(), enabled);
            true
        })
    }
}

/// Forget every remembered app — a full reset.
///
/// # Safety
/// `handle` must come from [`super::funput_app_language_new`] or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_app_language_clear(handle: *mut FunputAppLanguage) {
    unsafe {
        with_mut(handle, |state| {
            state.remembered.clear();
        })
    }
}

/// Forget one app. Returns `true` if an entry was removed.
///
/// # Safety
/// `id` must point to at least `id_len` readable bytes, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_app_language_forget(
    handle: *mut FunputAppLanguage,
    id: *const u8,
    id_len: usize,
) -> bool {
    unsafe {
        with_mut(handle, |state| {
            let Some(id) = str_from_raw(id, id_len) else {
                return false;
            };
            state.remembered.remove(id).is_some()
        })
    }
}

#[cfg(test)]
mod tests {
    use super::super::focus::funput_app_language_note_focus;
    use super::super::handle::{funput_app_language_free, funput_app_language_new};
    use super::super::types::{APP_LANG_ENGLISH, APP_LANG_UNKNOWN};
    use super::*;

    fn seed(handle: *mut FunputAppLanguage, id: &str, enabled: bool) -> bool {
        unsafe { funput_app_language_seed(handle, id.as_ptr(), id.len(), enabled) }
    }

    fn note_focus(handle: *mut FunputAppLanguage, id: &str) -> i32 {
        unsafe { funput_app_language_note_focus(handle, id.as_ptr(), id.len()) }
    }

    #[test]
    fn empty_id_is_ignored() {
        let handle = funput_app_language_new();
        assert!(!seed(handle, "", true));
        unsafe { funput_app_language_free(handle) };
    }

    #[test]
    fn forget_removes_only_the_named_app() {
        let handle = funput_app_language_new();
        seed(handle, "a", true);
        seed(handle, "b", false);

        let forgot = unsafe { funput_app_language_forget(handle, b"a".as_ptr(), 1) };
        assert!(forgot);
        assert_eq!(note_focus(handle, "a"), APP_LANG_UNKNOWN);
        assert_eq!(note_focus(handle, "b"), APP_LANG_ENGLISH);

        unsafe { funput_app_language_free(handle) };
    }

    #[test]
    fn clear_wipes_everything() {
        let handle = funput_app_language_new();
        seed(handle, "a", true);
        seed(handle, "b", false);

        unsafe { funput_app_language_clear(handle) };
        assert_eq!(note_focus(handle, "a"), APP_LANG_UNKNOWN);
        assert_eq!(note_focus(handle, "b"), APP_LANG_UNKNOWN);

        unsafe { funput_app_language_free(handle) };
    }
}
