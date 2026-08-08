use crate::abi::safe;

use super::handle::{FunputAppLanguage, str_from_raw, with_mut};
use super::types::{APP_LANG_UNKNOWN, code_for};

/// Look up the remembered VI/EN state for the just-focused app `id`. Returns
/// [`super::APP_LANG_VIETNAMESE`] / [`super::APP_LANG_ENGLISH`] when this app has
/// a remembered choice, [`super::APP_LANG_UNKNOWN`] otherwise — the host should
/// only call `funput_set_enabled` in the former case, leaving the composition
/// engine's current state untouched for an app it has never seen toggled.
///
/// # Safety
/// `id` must point to at least `id_len` readable bytes, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_app_language_note_focus(
    handle: *const FunputAppLanguage,
    id: *const u8,
    id_len: usize,
) -> i32 {
    safe(APP_LANG_UNKNOWN, || {
        let Some(state) = (unsafe { handle.as_ref() }) else {
            return APP_LANG_UNKNOWN;
        };
        let Some(id) = (unsafe { str_from_raw(id, id_len) }) else {
            return APP_LANG_UNKNOWN;
        };
        state
            .remembered
            .get(id)
            .map_or(APP_LANG_UNKNOWN, |&enabled| code_for(enabled))
    })
}

/// Record a manual VI/EN toggle for the app the caller knows is currently
/// focused (e.g. a hotkey pressed while that app has focus). Returns `true` on
/// success — the host should then persist `(id, enabled)` to its own settings
/// store, since this engine never touches disk.
///
/// A toggle initiated from a surface that steals focus (a tray icon, the
/// Settings window) is a platform-side concern: resolve the target app id
/// there before calling this, the same way the per-app `pending`/deferred
/// overrides already work today.
///
/// # Safety
/// `id` must point to at least `id_len` readable bytes, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_app_language_note_toggle(
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

#[cfg(test)]
mod tests {
    use super::super::handle::{funput_app_language_free, funput_app_language_new};
    use super::super::memory::funput_app_language_seed;
    use super::super::types::{APP_LANG_ENGLISH, APP_LANG_UNKNOWN, APP_LANG_VIETNAMESE};
    use super::*;

    #[test]
    fn unseen_app_is_unknown() {
        let handle = funput_app_language_new();
        let code = unsafe { funput_app_language_note_focus(handle, b"a".as_ptr(), 1) };
        assert_eq!(code, APP_LANG_UNKNOWN);
        unsafe { funput_app_language_free(handle) };
    }

    #[test]
    fn seeded_app_resolves_to_its_remembered_state() {
        let handle = funput_app_language_new();
        unsafe { funput_app_language_seed(handle, b"a".as_ptr(), 1, true) };
        let code = unsafe { funput_app_language_note_focus(handle, b"a".as_ptr(), 1) };
        assert_eq!(code, APP_LANG_VIETNAMESE);
        unsafe { funput_app_language_free(handle) };
    }

    #[test]
    fn toggle_overwrites_a_seeded_state() {
        let handle = funput_app_language_new();
        unsafe { funput_app_language_seed(handle, b"a".as_ptr(), 1, true) };
        let toggled = unsafe { funput_app_language_note_toggle(handle, b"a".as_ptr(), 1, false) };
        assert!(toggled);
        let code = unsafe { funput_app_language_note_focus(handle, b"a".as_ptr(), 1) };
        assert_eq!(code, APP_LANG_ENGLISH);
        unsafe { funput_app_language_free(handle) };
    }

    #[test]
    fn toggle_with_empty_id_is_ignored() {
        let handle = funput_app_language_new();
        let toggled = unsafe { funput_app_language_note_toggle(handle, b"".as_ptr(), 0, true) };
        assert!(!toggled);
        unsafe { funput_app_language_free(handle) };
    }

    #[test]
    fn null_handle_is_safe() {
        assert_eq!(
            unsafe { funput_app_language_note_focus(std::ptr::null(), b"a".as_ptr(), 1) },
            APP_LANG_UNKNOWN
        );
        assert!(!unsafe {
            funput_app_language_note_toggle(std::ptr::null_mut(), b"a".as_ptr(), 1, true)
        });
    }
}
