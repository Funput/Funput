//! Composition operations exposed over the C ABI: feed keys, read the composed
//! buffer, and reset state. Each wraps [`support::with_engine_mut`] /
//! [`support::with_engine_ref`], which null-check the handle and guard against panics.

use crate::FunputEngine;
use crate::support;
use crate::types::FunputResult;

/// Arm capitalization for the next word — call on text-field focus so the first
/// letter typed (start of input) is capitalized. A no-op unless auto-capitalize is on.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_arm_capitalization(engine: *mut FunputEngine) {
    unsafe { support::with_engine_mut(engine, |e| e.arm_capitalization()) }
}

/// Reset composition state (buffer + raw keys), e.g. on focus change.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_clear(engine: *mut FunputEngine) {
    unsafe { support::with_engine_mut(engine, |e| e.clear()) }
}

/// Process one Unicode scalar. Returns the platform instruction by value.
///
/// A null handle or invalid `codepoint` yields [`FunputResult::none`].
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_process_char(
    engine: *mut FunputEngine,
    codepoint: u32,
) -> FunputResult {
    unsafe {
        support::with_engine_mut(engine, |e| match char::from_u32(codepoint) {
            Some(ch) => FunputResult::from_ime(&e.process_char(ch)),
            None => FunputResult::none(),
        })
    }
}

/// Copy the current composed buffer (the text the host shows as marked/underlined
/// composition) as UTF-32 into `out`, up to `cap` codepoints. Returns the number
/// of codepoints written.
///
/// Null-safe: a null handle or null `out` yields `0`.
///
/// # Safety
/// `engine` must be a valid handle or null. `out` must point to writable storage
/// for at least `cap` `u32` values, or be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_buffer(
    engine: *const FunputEngine,
    out: *mut u32,
    cap: usize,
) -> usize {
    unsafe {
        support::with_engine_ref(engine, |e| {
            if out.is_null() {
                return 0;
            }
            let dst = std::slice::from_raw_parts_mut(out, cap);
            support::copy_codepoints(dst, e.buffer().chars())
        })
    }
}

/// Backspace inside the current composition: drop the last composed character so
/// the next keystroke composes against the corrected text (`Phua` ⌫ `s` → `Phú`).
///
/// Returns a no-op result — the host passes the Backspace through to delete one
/// character in the app.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_backspace(engine: *mut FunputEngine) -> FunputResult {
    unsafe { support::with_engine_mut(engine, |e| FunputResult::from_ime(&e.on_backspace())) }
}

/// Flip the word being composed between its Vietnamese form and its raw keystrokes
/// (`card` ⇄ `cải`), and back on a second call. Returns the delete+inject the host
/// should apply (`ACTION_SEND`), or [`FunputResult::none`] when there is nothing to
/// flip. Hosts that show marked text can ignore the payload and re-render
/// [`funput_buffer`] after a non-`ACTION_NONE` result.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_flip_composing(engine: *mut FunputEngine) -> FunputResult {
    unsafe { support::with_engine_mut(engine, |e| FunputResult::from_ime(&e.flip_composing())) }
}
