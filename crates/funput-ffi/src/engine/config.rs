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
        // Not in `FunputConfig`: this struct crosses the C ABI by value, and the
        // Swift side declaring it is built separately, so growing it here would
        // mismatch silently until every consumer is rebuilt. The hosts on this ABI
        // have no gõ tắt switch yet, and `true` is what they behaved as before —
        // give them a dedicated setter when one of them grows the feature.
        shortcuts_enabled: true,
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
