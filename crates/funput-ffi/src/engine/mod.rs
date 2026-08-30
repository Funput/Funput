//! Composition-engine C ABI: the opaque [`FunputEngine`] handle and the `funput_*`
//! calls that drive it.
//!
//! - [`compose`] — feed keys and read/edit the composing buffer.
//! - [`config`] — method, tone style, and the boolean feature toggles.
//! - [`shortcuts`] — the gõ tắt (text-expansion) table.
//! - [`result`] — the POD [`FunputResult`] returned by value from each keystroke.
//!
//! The handle mirrors [`crate::suggestion`]'s: an opaque `*mut` boxed over a
//! `funput_engine::Engine`, created by [`funput_engine_new`] and released by
//! [`funput_engine_free`].

mod compose;
mod config;
mod result;
mod shortcuts;

use funput_engine::Engine;

pub use compose::{
    SOURCE_NUMPAD, SOURCE_STANDARD, funput_adopt, funput_arm_capitalization, funput_backspace,
    funput_buffer, funput_clear, funput_flip_composing, funput_process_char, funput_process_key,
};
pub use config::{
    FunputConfig, METHOD_TELEX, METHOD_TELEX_ADVANCED, METHOD_VNI, funput_configure,
    funput_set_enabled, funput_set_method,
};
pub use result::{ACTION_NONE, ACTION_RESTORE, ACTION_SEND, CHARS_CAP, FunputResult};
pub use shortcuts::{funput_add_shortcut, funput_clear_shortcuts, funput_set_shortcut_smart_case};

/// Opaque IME engine handle for C callers. Create with [`funput_engine_new`],
/// release with [`funput_engine_free`]. cbindgen emits this as an opaque struct.
pub struct FunputEngine {
    pub(crate) inner: Engine,
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
