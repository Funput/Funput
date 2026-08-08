//! Tri-state result codes for [`super::funput_app_language_note_focus`].

/// No remembered state for this app — the host should leave the composition
/// engine's current VI/EN state untouched.
pub const APP_LANG_UNKNOWN: i32 = -1;
/// The app is remembered as English (Vietnamese input suppressed).
pub const APP_LANG_ENGLISH: i32 = 0;
/// The app is remembered as Vietnamese.
pub const APP_LANG_VIETNAMESE: i32 = 1;

/// Map a remembered `bool` to its C-ABI code.
pub(crate) fn code_for(enabled: bool) -> i32 {
    if enabled {
        APP_LANG_VIETNAMESE
    } else {
        APP_LANG_ENGLISH
    }
}
