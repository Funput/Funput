//! C ABI boundary for `funput-engine`.
//!
//! Lets a non-Rust shell (Swift IMKit, C# on Windows, a C terminal interposer…)
//! drive the Vietnamese IME engine. A Rust consumer should link `funput-engine`
//! directly instead.
//!
//! # Model
//!
//! Handle-based: [`funput_engine_new`] returns an opaque `*mut FunputEngine`; pass
//! it to every call and release it with [`funput_engine_free`]. Results come back
//! by value as [`FunputResult`] (POD, no allocation, no free).
//!
//! # Layout
//!
//! Two independent C APIs over shared plumbing; each submodule re-exports flat here
//! so the C header stays one namespace:
//!
//! - `engine/` — the composition IME ([`FunputEngine`] + the `funput_*` calls).
//! - `suggestion/` — the personal-suggestion store ([`FunputSuggestionEngine`] +
//!   the `funput_suggestion_*` calls), independent of composition.
//! - `app_language/` — per-app VI/EN memory ([`FunputAppLanguage`] + the
//!   `funput_app_language_*` calls), independent of composition too; the host
//!   wires its decision into [`funput_set_enabled`] itself.
//! - `abi/` — the shared panic guard, null-handle check, and UTF-32 marshalling.
//!
//! # Safety
//!
//! All functions are null-safe: a null handle (or invalid codepoint) yields a
//! no-op [`FunputResult::none`] / does nothing. The caller must not use a handle
//! after freeing it, and must not free the same handle twice.
//!
//! Every entry point that touches the engine runs inside `abi`'s panic guard, so a
//! panic in the engine is caught at the boundary and turned into a no-op result —
//! it never unwinds into the host (which would abort the whole IME process).

mod abi;
mod app_language;
mod engine;
mod suggestion;

pub use app_language::{
    APP_LANG_ENGLISH, APP_LANG_UNKNOWN, APP_LANG_VIETNAMESE, FunputAppLanguage,
    funput_app_language_clear, funput_app_language_forget, funput_app_language_free,
    funput_app_language_new, funput_app_language_note_focus, funput_app_language_note_toggle,
    funput_app_language_seed,
};
pub use engine::{
    ACTION_NONE, ACTION_RESTORE, ACTION_SEND, CHARS_CAP, FunputConfig, FunputEngine, FunputResult,
    METHOD_TELEX, METHOD_TELEX_ADVANCED, METHOD_VNI, SOURCE_NUMPAD, SOURCE_STANDARD,
    funput_add_shortcut, funput_adopt, funput_arm_capitalization, funput_backspace, funput_buffer,
    funput_clear, funput_clear_shortcuts, funput_configure, funput_engine_free, funput_engine_new,
    funput_flip_composing, funput_process_char, funput_process_key, funput_set_enabled,
    funput_set_method,
};
pub use suggestion::{
    FunputSuggestionCandidate, FunputSuggestionEngine, FunputSuggestionResult,
    FunputSuggestionStats, SUGGESTION_CAP, SUGGESTION_CHARS_CAP, funput_suggestion_compact,
    funput_suggestion_engine_free, funput_suggestion_engine_new_in_memory,
    funput_suggestion_engine_open, funput_suggestion_flush, funput_suggestion_learn,
    funput_suggestion_query, funput_suggestion_reset, funput_suggestion_stats,
};
