//! Gõ tắt for a key the host sends on to the app itself: Enter, which may send a
//! message, and Tab, which may move focus or accept a completion.
//!
//! A word boundary typed into a word expands through `funput_process_key_text` and
//! is swallowed with it. Enter and Tab cannot be: the app must still receive them.
//! So the host asks first, writes the expansion in place of the word, and lets the
//! key through. Whether a key asks at all is the host's setting, not the engine's.

use std::ffi::c_void;

use crate::{FunputEngine, FunputResult, abi};

/// Expand the word being composed if it is a gõ tắt trigger, without a boundary
/// character, for a key the host passes on to the app (Enter, Tab).
///
/// On a match the result is `ACTION_SEND`: `backspace` counts the Unicode scalars
/// of the word to replace, and the expansion is delivered to `receive` as UTF-8, like
/// `funput_process_key_text`. The word is then committed. Only gõ tắt runs: no
/// English restore, no typo correction, and no Backspace undo is armed, since the key
/// that follows may already have sent the text away.
///
/// No match, nothing composing, or a null handle returns a no-op and calls nothing;
/// the host then commits the word as it does for any key that moves the caret.
///
/// # Safety
/// `engine` must be a valid handle or null. `receive` must accept `context` and a
/// borrowed UTF-8 pointer and length, must not re-enter or free the engine, and must
/// not unwind across the C boundary.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_expand_shortcut(
    engine: *mut FunputEngine,
    receive: Option<unsafe extern "C" fn(*mut c_void, *const u8, usize)>,
    context: *mut c_void,
) -> FunputResult {
    unsafe {
        abi::with_engine_mut(engine, |e| {
            let Some(result) = e.expand_shortcut() else {
                return FunputResult::none();
            };
            if let Some(receive) = receive {
                receive(context, result.output.as_ptr(), result.output.len());
            }
            FunputResult::from_ime(&result)
        })
    }
}

#[cfg(test)]
mod tests {
    use super::super::testing::{add, buffer, collect, type_keys};
    use super::*;
    use crate::*;

    fn expand(engine: *mut FunputEngine) -> (FunputResult, String) {
        let mut text = String::new();
        let context = (&mut text as *mut String).cast();
        let result = unsafe { funput_engine_expand_shortcut(engine, Some(collect), context) };
        (result, text)
    }

    #[test]
    fn a_trigger_expands_without_a_boundary_and_arms_no_undo() {
        unsafe {
            let engine = funput_engine_new();
            add(engine, "ct", "cơm tấm");
            type_keys(engine, "ct");
            let (result, text) = expand(engine);
            assert_eq!(result.action, ACTION_SEND);
            assert_eq!(result.backspace, 2);
            assert_eq!(text, "cơm tấm");
            assert!(buffer(engine).is_empty(), "word committed");
            assert!(!funput_engine_shortcut_undo_text(
                engine,
                None,
                std::ptr::null_mut()
            ));
            funput_engine_free(engine);
        }
    }

    #[test]
    fn a_word_that_is_no_trigger_is_left_to_the_host() {
        unsafe {
            let engine = funput_engine_new();
            add(engine, "ct", "cơm tấm");
            type_keys(engine, "co");
            let (result, text) = expand(engine);
            assert_eq!(result.action, ACTION_NONE);
            assert!(text.is_empty(), "nothing delivered");
            assert_eq!(buffer(engine), "co", "word kept");
            funput_engine_free(engine);
        }
    }

    #[test]
    fn english_mode_follows_the_english_switch() {
        unsafe {
            let engine = funput_engine_new();
            add(engine, "ct", "cơm tấm");
            funput_set_enabled(engine, false);
            for on in [false, true] {
                funput_clear(engine);
                funput_set_shortcuts_in_english(engine, on);
                type_keys(engine, "ct");
                let action = expand(engine).0.action;
                assert_eq!(action, if on { ACTION_SEND } else { ACTION_NONE });
            }
            funput_engine_free(engine);
        }
    }

    #[test]
    fn an_expansion_longer_than_the_result_array_arrives_whole() {
        unsafe {
            let engine = funput_engine_new();
            let expansion = "địa chỉ ".repeat(CHARS_CAP);
            add(engine, "dc", &expansion);
            type_keys(engine, "dc");
            assert_eq!(expand(engine).1, expansion);
            funput_engine_free(engine);
        }
    }

    #[test]
    fn a_null_handle_or_callback_is_safe() {
        unsafe {
            let (result, text) = expand(std::ptr::null_mut());
            assert_eq!(result.action, ACTION_NONE);
            assert!(text.is_empty());

            let engine = funput_engine_new();
            add(engine, "ct", "cơm tấm");
            type_keys(engine, "ct");
            let result = funput_engine_expand_shortcut(engine, None, std::ptr::null_mut());
            assert_eq!(result.action, ACTION_SEND, "a null callback still expands");
            funput_engine_free(engine);
        }
    }
}
