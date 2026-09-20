//! Editing a word that is already on screen: Backspace, the flip hotkey, and
//! re-opening a committed word.

use crate::abi;
use crate::engine::FunputEngine;
use crate::engine::result::FunputResult;

/// Backspace inside the current composition: drop the last composed character so the
/// next keystroke composes against the corrected text (`Phua` ⌫ `s` → `Phú`). Normally
/// returns a no-op result and the host passes the Backspace through to delete its own
/// character.
///
/// One exception: with typo correction on, a Backspace straight after a correction
/// undoes it, and this returns an `ACTION_SEND` that puts back what the user typed.
/// A host that passes the key through as well would then eat one character too many,
/// so it must check `funput_engine_has_correction_undo` first — or simply stop
/// passing the key through whenever the result is not `ACTION_NONE`.
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
/// [`funput_add_shortcut`](crate::funput_add_shortcut). Returns whether it was taken — only
/// a Vietnamese syllable is, so leave the document alone on `false`.
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
