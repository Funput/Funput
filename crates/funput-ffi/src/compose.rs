//! Composition operations exposed over the C ABI: feed keys, read the composed
//! buffer, and reset state.

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
    support::safe((), || {
        if let Some(engine) = unsafe { engine.as_mut() } {
            engine.inner.arm_capitalization();
        }
    })
}

/// Reset composition state (buffer + raw keys), e.g. on focus change.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_clear(engine: *mut FunputEngine) {
    support::safe((), || {
        if let Some(engine) = unsafe { engine.as_mut() } {
            engine.inner.clear();
        }
    })
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
    support::safe(FunputResult::none(), || {
        let Some(engine) = (unsafe { engine.as_mut() }) else {
            return FunputResult::none();
        };
        let Some(ch) = char::from_u32(codepoint) else {
            return FunputResult::none();
        };
        FunputResult::from_ime(&engine.inner.process_char(ch))
    })
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
    support::safe(0, || {
        let Some(engine) = (unsafe { engine.as_ref() }) else {
            return 0;
        };
        if out.is_null() {
            return 0;
        }
        let mut count = 0;
        for ch in engine.inner.buffer().chars() {
            if count >= cap {
                break;
            }
            unsafe { *out.add(count) = ch as u32 };
            count += 1;
        }
        count
    })
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
    support::safe(FunputResult::none(), || {
        let Some(engine) = (unsafe { engine.as_mut() }) else {
            return FunputResult::none();
        };
        FunputResult::from_ime(&engine.inner.on_backspace())
    })
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
    support::safe(FunputResult::none(), || {
        let Some(engine) = (unsafe { engine.as_mut() }) else {
            return FunputResult::none();
        };
        FunputResult::from_ime(&engine.inner.flip_composing())
    })
}
