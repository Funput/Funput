//! Composition operations exposed over the C ABI: feed keys, read the composed
//! buffer, and reset state. Each wraps [`abi::with_engine_mut`] /
//! [`abi::with_engine_ref`], which null-check the handle and guard against panics.

use funput_engine::KeySource;

use super::FunputEngine;
use super::result::FunputResult;
use crate::abi;

/// [`funput_process_key`] source: main keyboard — digits may act as VNI modifiers.
pub const SOURCE_STANDARD: u32 = 0;
/// [`funput_process_key`] source: numeric keypad — a digit is always a literal number.
pub const SOURCE_NUMPAD: u32 = 1;

/// Decode the C `source` argument; any unknown value falls back to the main keyboard.
fn decode_source(source: u32) -> KeySource {
    match source {
        SOURCE_NUMPAD => KeySource::Numpad,
        _ => KeySource::Standard,
    }
}

/// Arm capitalization for the next word — call on text-field focus so the first
/// letter typed (start of input) is capitalized. A no-op unless auto-capitalize is on.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_arm_capitalization(engine: *mut FunputEngine) {
    unsafe { abi::with_engine_mut(engine, |e| e.arm_capitalization()) }
}

/// Reset composition state (buffer + raw keys), e.g. on focus change.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_clear(engine: *mut FunputEngine) {
    unsafe { abi::with_engine_mut(engine, |e| e.clear()) }
}

/// Process one Unicode scalar from the main keyboard — [`funput_process_key`] with
/// [`SOURCE_STANDARD`]. A null handle or invalid `codepoint` yields
/// [`FunputResult::none`].
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_process_char(
    engine: *mut FunputEngine,
    codepoint: u32,
) -> FunputResult {
    unsafe { funput_process_key(engine, codepoint, SOURCE_STANDARD) }
}

/// Process one Unicode scalar tagged with the physical key `source`
/// ([`SOURCE_STANDARD`] or [`SOURCE_NUMPAD`]). Returns the platform instruction by
/// value.
///
/// A numpad digit (`SOURCE_NUMPAD` + `0`–`9`) is emitted as a literal number and
/// ends the current word instead of applying a VNI tone/shape. A null handle or
/// invalid `codepoint` yields [`FunputResult::none`]; an unknown `source` is
/// treated as [`SOURCE_STANDARD`].
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_process_key(
    engine: *mut FunputEngine,
    codepoint: u32,
    source: u32,
) -> FunputResult {
    unsafe {
        abi::with_engine_mut(engine, |e| match char::from_u32(codepoint) {
            Some(ch) => FunputResult::from_ime(&e.process_key(ch, decode_source(source))),
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
        abi::with_engine_ref(engine, |e| {
            if out.is_null() {
                return 0;
            }
            let dst = std::slice::from_raw_parts_mut(out, cap);
            abi::copy_codepoints(dst, e.buffer().chars())
        })
    }
}

/// Backspace inside the current composition: drop the last composed character so the
/// next keystroke composes against the corrected text (`Phua` ⌫ `s` → `Phú`). Returns a
/// no-op result — the host passes the Backspace through to delete its own character.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_backspace(engine: *mut FunputEngine) -> FunputResult {
    unsafe { abi::with_engine_mut(engine, |e| FunputResult::from_ime(&e.on_backspace())) }
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
    unsafe { abi::with_engine_mut(engine, |e| FunputResult::from_ime(&e.flip_composing())) }
}

/// Re-open an already-committed word as the live composition, so the next keystroke
/// edits it (Backspace back onto `chào`, then `s` gives `cháo`). `word` is UTF-32, as in
/// [`funput_add_shortcut`](crate::funput_add_shortcut). Returns whether it was taken —
/// only a complete Vietnamese syllable is, so leave the document alone on `false`.
///
/// # Safety
/// `engine` must be a valid handle or null; `word` must point to `len` `u32` values or
/// be null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_adopt(
    engine: *mut FunputEngine,
    word: *const u32,
    len: usize,
) -> bool {
    unsafe {
        let text = abi::string_from_utf32(word, len);
        abi::with_engine_mut(engine, |e| e.adopt(&text))
    }
}
