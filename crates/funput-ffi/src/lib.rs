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
//! The exports are grouped by concern into the `config`, `compose`, and `shortcuts`
//! modules, then re-exported here so the crate presents a single flat C surface.
//!
//! # Safety
//!
//! All functions are null-safe: a null handle (or invalid codepoint) yields a
//! no-op [`FunputResult::none`] / does nothing. The caller must not use a handle
//! after freeing it, and must not free the same handle twice.
//!
//! Every entry point that touches the engine runs inside [`support::safe`], so a
//! panic in the engine is caught at the boundary and turned into a no-op result —
//! it never unwinds into the host (which would abort the whole IME process).

mod config;
mod compose;
mod shortcuts;
mod support;
mod types;

use funput_engine::Engine;

pub use compose::{
    funput_arm_capitalization, funput_backspace, funput_buffer, funput_clear,
    funput_flip_composing, funput_process_char,
};
pub use config::{
    funput_set_auto_capitalize, funput_set_eager_restore, funput_set_enabled, funput_set_method,
    funput_set_smart_restore, funput_set_spell_check, funput_set_tone_style,
};
pub use shortcuts::{funput_add_shortcut, funput_clear_shortcuts};
pub use types::{ACTION_NONE, ACTION_RESTORE, ACTION_SEND, CHARS_CAP, FunputResult};

/// Opaque IME engine handle for C callers. Create with [`funput_engine_new`],
/// release with [`funput_engine_free`]. cbindgen emits this as an opaque struct.
pub struct FunputEngine {
    inner: Engine,
}

/// Create a new engine. Release it with [`funput_engine_free`].
#[unsafe(no_mangle)]
pub extern "C" fn funput_engine_new() -> *mut FunputEngine {
    Box::into_raw(Box::new(FunputEngine {
        inner: Engine::new(),
    }))
}

/// Free an engine created by [`funput_engine_new`]. Null is ignored.
///
/// # Safety
/// `engine` must come from [`funput_engine_new`] and not be freed already.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_free(engine: *mut FunputEngine) {
    if !engine.is_null() {
        drop(unsafe { Box::from_raw(engine) });
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn flip_composing_on_fresh_engine_is_noop() {
        let engine = funput_engine_new();
        let result = unsafe { funput_flip_composing(engine) };
        assert_eq!(result.action, ACTION_NONE); // nothing composing
        unsafe { funput_engine_free(engine) };
    }
}
