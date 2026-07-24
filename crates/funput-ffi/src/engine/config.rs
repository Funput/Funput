//! Engine configuration setters exposed over the C ABI.
//!
//! Each is a thin wrapper over [`abi::with_engine_mut`], which folds the
//! null-handle check and the `catch_unwind` panic guard into one place so the
//! boundary rule "every FFI call goes through the guard" holds uniformly.

use funput_core::{InputMethod, ToneStyle};
use funput_engine::EngineConfig;

use super::FunputEngine;
use crate::abi;

pub const METHOD_TELEX: u8 = 0;
pub const METHOD_VNI: u8 = 1;
pub const METHOD_TELEX_ADVANCED: u8 = 2;
const TONE_STYLE_MODERN: u8 = 1;

/// Decode the wire method byte; any unknown value falls back to standard Telex.
fn decode_method(method: u8) -> InputMethod {
    match method {
        METHOD_VNI => InputMethod::Vni,
        METHOD_TELEX_ADVANCED => InputMethod::TelexAdvanced,
        _ => InputMethod::Telex,
    }
}

/// Decode the wire tone-style byte (`1 = Modern`, anything else = Traditional).
fn decode_tone_style(style: u8) -> ToneStyle {
    if style == TONE_STYLE_MODERN {
        ToneStyle::Modern
    } else {
        ToneStyle::Traditional
    }
}

/// Set the input method: `0 = Telex`, `1 = VNI`, `2 = Telex Advanced`.
/// Any other value falls back to standard Telex.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_method(engine: *mut FunputEngine, method: u8) {
    let method = decode_method(method);
    unsafe { abi::with_engine_mut(engine, |e| e.set_method(method)) }
}

/// Set the tone-mark placement style: `0 = Traditional` (`hòa`), `1 = Modern`
/// (`hoà`) — any other value = Traditional.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_tone_style(engine: *mut FunputEngine, style: u8) {
    let style = decode_tone_style(style);
    unsafe { abi::with_engine_mut(engine, |e| e.set_tone_style(style)) }
}

/// A whole engine configuration passed by value over the C ABI — the six user
/// options. `enabled` and the gõ tắt shortcut table have their own functions.
#[repr(C)]
#[derive(Clone, Copy)]
pub struct FunputConfig {
    /// `0 = Telex`, `1 = VNI`, `2 = Telex Advanced` (see `METHOD_*`).
    pub method: u8,
    /// `0 = Traditional`, `1 = Modern`.
    pub tone_style: u8,
    pub smart_restore: bool,
    pub eager_restore: bool,
    pub spell_check: bool,
    pub auto_capitalize: bool,
}

/// Apply a whole [`FunputConfig`] at once — the batch equivalent of the individual
/// `funput_set_*` functions, with the same side effects (a method change clears the
/// composition; auto-capitalize off resets its tracking). `funput_set_enabled` and
/// the shortcut table are separate and left untouched.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_configure(engine: *mut FunputEngine, config: FunputConfig) {
    let cfg = EngineConfig {
        method: decode_method(config.method),
        tone_style: decode_tone_style(config.tone_style),
        smart_restore: config.smart_restore,
        eager_restore: config.eager_restore,
        spell_check: config.spell_check,
        auto_capitalize: config.auto_capitalize,
    };
    unsafe { abi::with_engine_mut(engine, |e| e.configure(cfg)) }
}

/// Enable or disable Vietnamese composition.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_enabled(engine: *mut FunputEngine, enabled: bool) {
    unsafe { abi::with_engine_mut(engine, |e| e.set_enabled(enabled)) }
}

/// Toggle auto-restore of non-Vietnamese words to their raw Latin keystrokes
/// (`card` stays `card`). When off, the composed buffer is always kept.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_smart_restore(engine: *mut FunputEngine, on: bool) {
    unsafe { abi::with_engine_mut(engine, |e| e.set_smart_restore(on)) }
}

/// Toggle eager restore — flip to raw keys the instant a word dead-ends instead of
/// waiting for a word boundary. Only applies while smart restore is on.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_eager_restore(engine: *mut FunputEngine, on: bool) {
    unsafe { abi::with_engine_mut(engine, |e| e.set_eager_restore(on)) }
}

/// Toggle spell-check ("Kiểm tra chính tả") — only place a diacritic when the result
/// can still become a real Vietnamese syllable, otherwise keep the modifier key as a
/// literal. Off by default.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_spell_check(engine: *mut FunputEngine, on: bool) {
    unsafe { abi::with_engine_mut(engine, |e| e.set_spell_check(on)) }
}

/// Toggle auto-capitalize ("Tự động viết hoa") — uppercase the first letter of a word
/// that starts a sentence. Off by default; a no-op while off.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_auto_capitalize(engine: *mut FunputEngine, on: bool) {
    unsafe { abi::with_engine_mut(engine, |e| e.set_auto_capitalize(on)) }
}
