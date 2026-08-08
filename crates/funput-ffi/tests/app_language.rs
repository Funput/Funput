//! Round-trip tests: drive the per-app VI/EN memory `extern "C"` API exactly
//! like a C caller would.

use funput_ffi::{
    APP_LANG_ENGLISH, APP_LANG_UNKNOWN, APP_LANG_VIETNAMESE, funput_app_language_clear,
    funput_app_language_forget, funput_app_language_free, funput_app_language_new,
    funput_app_language_note_focus, funput_app_language_note_toggle, funput_app_language_seed,
};

fn seed(handle: *mut funput_ffi::FunputAppLanguage, id: &str, enabled: bool) -> bool {
    unsafe { funput_app_language_seed(handle, id.as_ptr(), id.len(), enabled) }
}

fn note_focus(handle: *const funput_ffi::FunputAppLanguage, id: &str) -> i32 {
    unsafe { funput_app_language_note_focus(handle, id.as_ptr(), id.len()) }
}

fn note_toggle(handle: *mut funput_ffi::FunputAppLanguage, id: &str, enabled: bool) -> bool {
    unsafe { funput_app_language_note_toggle(handle, id.as_ptr(), id.len(), enabled) }
}

#[test]
fn remembers_and_replays_per_app_state_through_c_abi() {
    let handle = funput_app_language_new();
    assert!(!handle.is_null());

    // A never-seen app has no opinion — the host must leave the engine as-is.
    assert_eq!(note_focus(handle, "com.apple.Terminal"), APP_LANG_UNKNOWN);

    // A manual toggle while that app is focused is remembered from then on.
    assert!(note_toggle(handle, "com.apple.Terminal", false));
    assert_eq!(note_focus(handle, "com.apple.Terminal"), APP_LANG_ENGLISH);

    // Re-toggling the same app overwrites its remembered state.
    assert!(note_toggle(handle, "com.apple.Terminal", true));
    assert_eq!(
        note_focus(handle, "com.apple.Terminal"),
        APP_LANG_VIETNAMESE
    );

    // Other apps are unaffected.
    assert_eq!(note_focus(handle, "com.apple.Safari"), APP_LANG_UNKNOWN);

    unsafe { funput_app_language_free(handle) };
}

#[test]
fn seed_restores_state_persisted_by_the_host() {
    let handle = funput_app_language_new();
    // A host loads its own settings file at startup and seeds each row, the
    // same way `funput_add_shortcut` mirrors a config file into the engine.
    assert!(seed(handle, "code.exe", false));
    assert!(seed(handle, "slack.exe", true));

    assert_eq!(note_focus(handle, "code.exe"), APP_LANG_ENGLISH);
    assert_eq!(note_focus(handle, "slack.exe"), APP_LANG_VIETNAMESE);

    unsafe { funput_app_language_free(handle) };
}

#[test]
fn forget_and_clear_drop_remembered_state() {
    let handle = funput_app_language_new();
    seed(handle, "a", true);
    seed(handle, "b", false);

    assert!(unsafe { funput_app_language_forget(handle, b"a".as_ptr(), 1) });
    assert_eq!(note_focus(handle, "a"), APP_LANG_UNKNOWN);
    assert_eq!(note_focus(handle, "b"), APP_LANG_ENGLISH);

    unsafe { funput_app_language_clear(handle) };
    assert_eq!(note_focus(handle, "b"), APP_LANG_UNKNOWN);

    unsafe { funput_app_language_free(handle) };
}

#[test]
fn null_and_empty_inputs_are_safe() {
    // Null handle: must not crash, and must resolve to "unknown"/failure.
    unsafe { funput_app_language_free(std::ptr::null_mut()) };
    assert_eq!(
        unsafe { funput_app_language_note_focus(std::ptr::null(), b"a".as_ptr(), 1) },
        APP_LANG_UNKNOWN
    );
    assert!(!unsafe {
        funput_app_language_note_toggle(std::ptr::null_mut(), b"a".as_ptr(), 1, true)
    });
    assert!(!unsafe { funput_app_language_seed(std::ptr::null_mut(), b"a".as_ptr(), 1, true) });
    unsafe { funput_app_language_clear(std::ptr::null_mut()) };
    assert!(!unsafe { funput_app_language_forget(std::ptr::null_mut(), b"a".as_ptr(), 1) });

    // Empty id on a live handle: ignored, not a crash.
    let handle = funput_app_language_new();
    assert!(!seed(handle, "", true));
    assert!(!note_toggle(handle, "", true));
    unsafe { funput_app_language_free(handle) };
}
