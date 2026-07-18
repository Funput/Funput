//! Engine configuration setters exposed over the C ABI.
//!
//! Each is a thin wrapper over [`support::with_engine_mut`], which folds the
//! null-handle check and the `catch_unwind` panic guard into one place so the
//! boundary rule "every FFI call goes through the guard" holds uniformly.

use funput_core::{InputMethod, ToneStyle};

use crate::FunputEngine;
use crate::support;

pub const METHOD_TELEX: u8 = 0;
pub const METHOD_VNI: u8 = 1;
pub const METHOD_TELEX_ADVANCED: u8 = 2;
const TONE_STYLE_MODERN: u8 = 1;

/// Set the input method: `0 = Telex`, `1 = VNI`, `2 = Telex Advanced`.
/// Any other value falls back to standard Telex.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_method(engine: *mut FunputEngine, method: u8) {
    let method = match method {
        METHOD_VNI => InputMethod::Vni,
        METHOD_TELEX_ADVANCED => InputMethod::TelexAdvanced,
        _ => InputMethod::Telex,
    };
    unsafe { support::with_engine_mut(engine, |e| e.set_method(method)) }
}

/// Set the tone-mark placement style: `0 = Traditional` (`hòa`), `1 = Modern`
/// (`hoà`) — any other value = Traditional.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_tone_style(engine: *mut FunputEngine, style: u8) {
    let style = if style == TONE_STYLE_MODERN {
        ToneStyle::Modern
    } else {
        ToneStyle::Traditional
    };
    unsafe { support::with_engine_mut(engine, |e| e.set_tone_style(style)) }
}

/// Enable or disable Vietnamese composition.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_enabled(engine: *mut FunputEngine, enabled: bool) {
    unsafe { support::with_engine_mut(engine, |e| e.set_enabled(enabled)) }
}

/// Toggle auto-restore of non-Vietnamese words to their raw Latin keystrokes
/// (`card` stays `card`). When off, the composed buffer is always kept.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_smart_restore(engine: *mut FunputEngine, on: bool) {
    unsafe { support::with_engine_mut(engine, |e| e.set_smart_restore(on)) }
}

/// Toggle eager restore — flip to raw keys the instant a word dead-ends instead of
/// waiting for a word boundary. Only applies while smart restore is on.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_eager_restore(engine: *mut FunputEngine, on: bool) {
    unsafe { support::with_engine_mut(engine, |e| e.set_eager_restore(on)) }
}

/// Toggle spell-check ("Kiểm tra chính tả") — only place a diacritic when the result
/// can still become a real Vietnamese syllable, otherwise keep the modifier key as a
/// literal. Off by default.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_spell_check(engine: *mut FunputEngine, on: bool) {
    unsafe { support::with_engine_mut(engine, |e| e.set_spell_check(on)) }
}

/// Toggle auto-capitalize ("Tự động viết hoa") — uppercase the first letter of a word
/// that starts a sentence. Off by default; a no-op while off.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_auto_capitalize(engine: *mut FunputEngine, on: bool) {
    unsafe { support::with_engine_mut(engine, |e| e.set_auto_capitalize(on)) }
}
