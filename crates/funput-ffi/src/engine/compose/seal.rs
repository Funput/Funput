//! Sealing the word being composed: a host gesture that ends it exactly as shown,
//! such as a lone Ctrl tap on the desktop.

use std::ffi::c_void;

use crate::abi;
use crate::engine::FunputEngine;

/// End the word being composed exactly as it is shown, without running a word
/// boundary: no gõ tắt, no typo correction, no English restore, and nothing for the
/// host to delete or inject. The next key starts a new word right behind this one.
///
/// Returns `true` when a word was sealed. The shown word is delivered to a non-null
/// `receive` synchronously, as borrowed UTF-8 valid only during the call: a host that
/// shows marked text commits it, and a host that injects already has it on screen
/// and only needs it to offer back to [`funput_adopt`](crate::funput_adopt) when
/// Backspace returns to it. Returns `false`, and calls nothing, with nothing
/// composing or for a null handle.
///
/// # Safety
/// `engine` must be a valid handle or null. `receive` must accept `context` and a
/// borrowed UTF-8 pointer and length, must not re-enter or free the engine, and must
/// not unwind across the C boundary.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_seal(
    engine: *mut FunputEngine,
    receive: Option<unsafe extern "C" fn(*mut c_void, *const u8, usize)>,
    context: *mut c_void,
) -> bool {
    unsafe {
        abi::with_engine_mut(engine, |e| {
            let Some(shown) = e.seal() else {
                return false;
            };
            if let Some(receive) = receive {
                receive(context, shown.as_ptr(), shown.len());
            }
            true
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::*;

    unsafe extern "C" fn collect(context: *mut c_void, bytes: *const u8, len: usize) {
        unsafe {
            let output = &mut *context.cast::<String>();
            *output = String::from_utf8(std::slice::from_raw_parts(bytes, len).to_vec()).unwrap();
        }
    }

    fn type_keys(engine: *mut FunputEngine, keys: &str) {
        for key in keys.chars() {
            unsafe { funput_process_char(engine, key as u32) };
        }
    }

    fn seal(engine: *mut FunputEngine) -> (bool, String) {
        let mut text = String::new();
        let context = (&mut text as *mut String).cast();
        let sealed = unsafe { funput_engine_seal(engine, Some(collect), context) };
        (sealed, text)
    }

    #[test]
    fn the_shown_word_is_delivered_and_the_composition_ends() {
        unsafe {
            let engine = funput_engine_new();
            funput_set_method(engine, METHOD_TELEX);
            type_keys(engine, "haf");
            assert_eq!(seal(engine), (true, "hà".to_owned()));
            let mut left = [0u32; CHARS_CAP];
            assert_eq!(funput_buffer(engine, left.as_mut_ptr(), CHARS_CAP), 0);
            funput_engine_free(engine);
        }
    }

    #[test]
    fn nothing_composing_delivers_nothing() {
        unsafe {
            let engine = funput_engine_new();
            assert_eq!(seal(engine), (false, String::new()));
            funput_engine_free(engine);
        }
    }

    #[test]
    fn a_null_handle_or_callback_is_safe() {
        unsafe {
            assert_eq!(seal(std::ptr::null_mut()), (false, String::new()));

            let engine = funput_engine_new();
            type_keys(engine, "ha");
            assert!(
                funput_engine_seal(engine, None, std::ptr::null_mut()),
                "a null callback still seals"
            );
            assert!(!funput_engine_seal(engine, None, std::ptr::null_mut()));
            funput_engine_free(engine);
        }
    }
}
