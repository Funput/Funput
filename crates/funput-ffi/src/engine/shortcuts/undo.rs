//! One-tap undo of a gõ tắt expansion over the C ABI.
//!
//! The engine arms the undo when an expansion lands and spends it on the next key; a
//! host that sees the caret back right after the expansion can arm it again
//! ([`funput_engine_rearm_shortcut_undo`]). A host with a suggestion bar reads the word
//! the expansion replaced
//! ([`funput_engine_shortcut_undo_text`]) to name the `↩` chip, checks the document
//! still ends with what it wrote ([`funput_engine_shortcut_undo_written`]), and takes
//! the expansion back with [`funput_engine_undo_shortcut`] on Backspace or a tap.
//!
//! Text leaves through a UTF-8 callback, as in [`super::funput_process_key_text`],
//! never through `FunputResult.chars`: that array cuts at `CHARS_CAP` codepoints, and
//! a word a user typed can be longer.

use std::ffi::c_void;

use crate::{FunputEngine, FunputResult, abi};

/// Whether the last expansion can still be taken back, and the word it replaced.
///
/// Returns `true` while the undo is armed. With a non-null `receive`, the word the
/// expansion replaced (not the raw keys, and without the boundary character) is
/// delivered to it synchronously, as borrowed UTF-8 valid only during the call. A
/// null `receive` just asks the question. Returns `false`, and calls nothing, when
/// nothing is armed or the handle is null.
///
/// # Safety
/// `engine` must be a valid handle or null. `receive` must accept `context` and a
/// borrowed UTF-8 pointer and length, must not re-enter or free the engine, and must
/// not unwind across the C boundary.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_shortcut_undo_text(
    engine: *const FunputEngine,
    receive: Option<unsafe extern "C" fn(*mut c_void, *const u8, usize)>,
    context: *mut c_void,
) -> bool {
    unsafe {
        abi::with_engine_ref(engine, |e| {
            let Some(text) = e.shortcut_undo_text() else {
                return false;
            };
            if let Some(receive) = receive {
                receive(context, text.as_ptr(), text.len());
            }
            true
        })
    }
}

/// The text the last expansion wrote: the expansion plus its boundary character,
/// exactly what [`funput_engine_undo_shortcut`] deletes. A host that cannot keep its
/// own record of the expansion compares this with the text before its caret to know
/// the undo still applies.
///
/// Returns `true`, delivering the text to a non-null `receive` as borrowed UTF-8, while
/// the undo is armed; `false`, calling nothing, otherwise or for a null handle.
///
/// # Safety
/// `engine` must be a valid handle or null. `receive` follows the rules of
/// [`funput_engine_shortcut_undo_text`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_shortcut_undo_written(
    engine: *const FunputEngine,
    receive: Option<unsafe extern "C" fn(*mut c_void, *const u8, usize)>,
    context: *mut c_void,
) -> bool {
    unsafe {
        abi::with_engine_ref(engine, |e| {
            let Some(text) = e.shortcut_undo_written() else {
                return false;
            };
            if let Some(receive) = receive {
                receive(context, text.as_ptr(), text.len());
            }
            true
        })
    }
}

/// Take the last expansion back: delete it and put the word it replaced, plus the
/// same boundary character, in its place.
///
/// The returned `backspace` counts Unicode scalars to delete. The text to insert is
/// delivered to `receive` as UTF-8, like `funput_process_key_text`. The trigger the
/// user just refused is then kept as typed once, the next time it is typed.
///
/// Spends the undo whether or not `receive` is null, so a host must check that the
/// document still ends with the expansion **before** calling. Nothing armed, or a
/// null handle, returns a no-op and does not call `receive`.
///
/// # Safety
/// `engine` must be a valid handle or null. `receive` follows the rules of
/// [`funput_engine_shortcut_undo_text`].
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_undo_shortcut(
    engine: *mut FunputEngine,
    receive: Option<unsafe extern "C" fn(*mut c_void, *const u8, usize)>,
    context: *mut c_void,
) -> FunputResult {
    unsafe {
        abi::with_engine_mut(engine, |e| {
            let Some(result) = e.undo_shortcut() else {
                return FunputResult::none();
            };
            if let Some(receive) = receive {
                receive(context, result.output.as_ptr(), result.output.len());
            }
            FunputResult::from_ime(&result)
        })
    }
}

/// Arm the undo of the last expansion again, after the next key spent it: the user
/// typed on, then deleted back until the caret sits right after the expansion.
/// Returns whether an undo is now armed, ready for [`funput_engine_undo_shortcut`].
///
/// Only the host knows the caret is back at that exact spot rather than at the same
/// text typed elsewhere, so it checks before calling. `false` while a word is being
/// composed, once the expansion was taken back, dismissed or cleared, or for a null
/// handle.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_rearm_shortcut_undo(engine: *mut FunputEngine) -> bool {
    unsafe { abi::with_engine_mut(engine, |e| e.rearm_shortcut_undo()) }
}

/// Drop the undo, the remembered expansion and the one-time refusal: the caret left the
/// field, or the expansion is no longer what sits before it. A host that only offers the
/// one-key undo may also call this on any Backspace that is not the undo; one that uses
/// [`funput_engine_rearm_shortcut_undo`] must not, or there is nothing left to re-arm.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_engine_dismiss_shortcut_undo(engine: *mut FunputEngine) {
    unsafe { abi::with_engine_mut(engine, |e| e.dismiss_shortcut_undo()) }
}

#[cfg(test)]
mod tests {
    use super::super::testing::{add, collect, type_keys};
    use super::*;
    use crate::*;

    fn word(engine: *const FunputEngine) -> Option<String> {
        let mut text = String::new();
        let context = (&mut text as *mut String).cast();
        unsafe { funput_engine_shortcut_undo_text(engine, Some(collect), context) }.then_some(text)
    }

    fn written(engine: *const FunputEngine) -> Option<String> {
        let mut text = String::new();
        let context = (&mut text as *mut String).cast();
        unsafe { funput_engine_shortcut_undo_written(engine, Some(collect), context) }
            .then_some(text)
    }

    #[test]
    fn the_written_text_is_the_expansion_with_its_boundary_until_the_undo_is_spent() {
        unsafe {
            let engine = funput_engine_new();
            add(engine, "vn", "Việt Nam");
            assert_eq!(written(engine), None, "nothing armed before an expansion");

            type_keys(engine, "vn,");
            assert_eq!(written(engine).as_deref(), Some("Việt Nam,"));
            funput_engine_undo_shortcut(engine, None, std::ptr::null_mut());
            assert_eq!(written(engine), None, "spent by the undo");

            type_keys(engine, "x vn ");
            assert_eq!(written(engine).as_deref(), Some("Việt Nam "));
            type_keys(engine, "x");
            assert_eq!(written(engine), None, "spent by the next key");
            assert!(!funput_engine_shortcut_undo_written(
                std::ptr::null(),
                Some(collect),
                std::ptr::null_mut()
            ));
            funput_engine_free(engine);
        }
    }

    #[test]
    fn the_undo_names_the_word_then_takes_the_expansion_back_once() {
        unsafe {
            let engine = funput_engine_new();
            add(engine, "vn", "Việt Nam");
            assert_eq!(word(engine), None, "nothing armed before an expansion");

            type_keys(engine, "vn ");
            assert_eq!(word(engine).as_deref(), Some("vn"));
            assert!(funput_engine_shortcut_undo_text(
                engine,
                None,
                std::ptr::null_mut()
            ));

            let mut output = String::new();
            let context = (&mut output as *mut String).cast();
            let result = funput_engine_undo_shortcut(engine, Some(collect), context);
            assert_eq!(result.action, ACTION_SEND);
            assert_eq!(result.backspace, "Việt Nam ".chars().count() as u32);
            assert_eq!(output, "vn ");

            assert_eq!(word(engine), None, "spent");
            output.clear();
            let again = funput_engine_undo_shortcut(engine, Some(collect), context);
            assert_eq!(again.action, ACTION_NONE);
            assert!(output.is_empty(), "a spent undo calls nothing");
            funput_engine_free(engine);
        }
    }

    #[test]
    fn a_word_longer_than_the_result_array_arrives_whole() {
        unsafe {
            let engine = funput_engine_new();
            // `b` composes with nothing, so the keys stay the word.
            let trigger = "b".repeat(CHARS_CAP * 3);
            add(engine, &trigger, "x");
            type_keys(engine, &format!("{trigger} "));
            assert_eq!(word(engine).as_deref(), Some(trigger.as_str()));

            let mut output = String::new();
            let context = (&mut output as *mut String).cast();
            funput_engine_undo_shortcut(engine, Some(collect), context);
            assert_eq!(output, format!("{trigger} "));
            funput_engine_free(engine);
        }
    }

    #[test]
    fn rearming_brings_back_the_undo_the_next_key_spent() {
        unsafe {
            let engine = funput_engine_new();
            add(engine, "vn", "Việt Nam");
            assert!(
                !funput_engine_rearm_shortcut_undo(engine),
                "nothing expanded yet"
            );

            type_keys(engine, "vn ");
            type_keys(engine, "x");
            assert!(
                !funput_engine_rearm_shortcut_undo(engine),
                "a word is composed"
            );
            funput_backspace(engine);
            assert_eq!(written(engine), None, "spent by the next key");

            assert!(funput_engine_rearm_shortcut_undo(engine));
            assert_eq!(written(engine).as_deref(), Some("Việt Nam "));
            let result = funput_engine_undo_shortcut(engine, None, std::ptr::null_mut());
            assert_eq!(result.action, ACTION_SEND);
            assert!(!funput_engine_rearm_shortcut_undo(engine), "taken back");
            assert!(!funput_engine_rearm_shortcut_undo(std::ptr::null_mut()));
            funput_engine_free(engine);
        }
    }

    #[test]
    fn dismissing_drops_the_undo_and_the_refusal() {
        unsafe {
            let engine = funput_engine_new();
            add(engine, "vn", "Việt Nam");
            type_keys(engine, "vn ");
            funput_engine_undo_shortcut(engine, None, std::ptr::null_mut());
            funput_engine_dismiss_shortcut_undo(engine);

            type_keys(engine, "vn");
            assert_eq!(funput_process_char(engine, ' ' as u32).action, ACTION_SEND);
            funput_engine_dismiss_shortcut_undo(engine);
            assert_eq!(word(engine), None);
            funput_engine_free(engine);
        }
    }

    #[test]
    fn a_null_handle_or_callback_is_a_no_op() {
        unsafe {
            let mut text = String::new();
            let context = (&mut text as *mut String).cast();
            assert!(!funput_engine_shortcut_undo_text(
                std::ptr::null(),
                Some(collect),
                context
            ));
            let result = funput_engine_undo_shortcut(std::ptr::null_mut(), Some(collect), context);
            assert_eq!(result.action, ACTION_NONE);
            funput_engine_dismiss_shortcut_undo(std::ptr::null_mut());
            assert!(text.is_empty());

            let engine = funput_engine_new();
            add(engine, "vn", "Việt Nam");
            type_keys(engine, "vn ");
            let undone = funput_engine_undo_shortcut(engine, None, std::ptr::null_mut());
            assert_eq!(undone.action, ACTION_SEND, "a null callback still undoes");
            funput_engine_free(engine);
        }
    }
}
