//! Engine configuration setters exposed over the C ABI.
//!
//! Each is a thin wrapper over [`abi::with_engine_mut`], which folds the
//! null-handle check and the `catch_unwind` panic guard into one place so the
//! boundary rule "every FFI call goes through the guard" holds uniformly.

use funput_core::{ExtraOnsets, InputMethod, ToneStyle};

use super::FunputEngine;
use crate::abi;

pub const METHOD_TELEX: u8 = 0;
pub const METHOD_VNI: u8 = 1;
pub const METHOD_TELEX_ADVANCED: u8 = 2;
const TONE_STYLE_MODERN: u8 = 1;

/// Letters for [`funput_set_extra_onsets`], OR-ed into one byte. The wire's own
/// stable values — never the core's internal bits.
pub const ONSET_F: u8 = 1;
pub const ONSET_J: u8 = 2;
pub const ONSET_W: u8 = 4;
pub const ONSET_Z: u8 = 8;

/// Each wire letter bit and the onset it admits.
const ONSETS: [(u8, ExtraOnsets); 4] = [
    (ONSET_F, ExtraOnsets::F),
    (ONSET_J, ExtraOnsets::J),
    (ONSET_W, ExtraOnsets::W),
    (ONSET_Z, ExtraOnsets::Z),
];

/// Decode the wire method byte; any unknown value falls back to standard Telex.
fn decode_method(method: u8) -> InputMethod {
    match method {
        METHOD_VNI => InputMethod::Vni,
        METHOD_TELEX_ADVANCED => InputMethod::TelexAdvanced,
        _ => InputMethod::Telex,
    }
}

/// Decode the wire letter mask; bits no `ONSET_*` names are ignored.
fn decode_onsets(letters: u8) -> ExtraOnsets {
    ONSETS
        .iter()
        .filter(|&&(bit, _)| letters & bit != 0)
        .fold(ExtraOnsets::NONE, |set, &(_, onset)| set.union(onset))
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
/// options. `enabled` and the gõ tắt options have their own functions: this struct
/// crosses the ABI by value and the hosts declaring it are built separately from the
/// header, so growing it would mismatch silently until every consumer is rebuilt.
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
/// composition; auto-capitalize off resets its tracking). `funput_set_enabled`, the
/// shortcut table, and the gõ tắt options are separate and left untouched.
///
/// Edits the live config rather than replacing it, so an option that is not on the
/// wire keeps whatever its own setter last put there, whichever order the two are
/// called in.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_configure(engine: *mut FunputEngine, config: FunputConfig) {
    unsafe {
        abi::with_engine_mut(engine, |e| {
            e.update_config(|cfg| {
                cfg.method = decode_method(config.method);
                cfg.tone_style = decode_tone_style(config.tone_style);
                cfg.smart_restore = config.smart_restore;
                cfg.eager_restore = config.eager_restore;
                cfg.spell_check = config.spell_check;
                cfg.auto_capitalize = config.auto_capitalize;
            })
        })
    }
}

/// Enable or disable Vietnamese composition.
///
/// Disabling does not make [`funput_process_key`] a no-op: a loaded gõ tắt table
/// still expands at a word boundary. Composition itself — diacritics, English
/// restore, auto-capitalize, flip, adopt — stops. Hosts can disable English-mode
/// expansion with `funput_set_shortcuts_in_english(false)`.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_enabled(engine: *mut FunputEngine, enabled: bool) {
    unsafe { abi::with_engine_mut(engine, |e| e.set_enabled(enabled)) }
}

/// Admit `z`, `f`, `w`, `j` as initial consonants (`zô`, `fải`, `wá`, `jờ`) — the
/// `ONSET_*` letters OR-ed together; `0` (the default) admits none. Bits no
/// `ONSET_*` names are ignored.
///
/// Its own function rather than a [`FunputConfig`] field, so `funput_configure`
/// leaves it alone. Off by default because it also lets English with a Vietnamese
/// rhyme compose (Telex `fast` → `fát`).
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_extra_onsets(engine: *mut FunputEngine, letters: u8) {
    let onsets = decode_onsets(letters);
    unsafe {
        abi::with_engine_mut(engine, |e| {
            e.update_config(|c| c.syllable_rules = c.syllable_rules.with_extra_onsets(onsets))
        })
    }
}

#[cfg(test)]
mod tests {
    use super::decode_onsets;
    use crate::*;
    use funput_core::ExtraOnsets;

    fn typed(engine: *mut FunputEngine, keys: &str) -> String {
        let mut text = String::new();
        unsafe {
            funput_clear(engine);
            for key in keys.chars() {
                let result = funput_process_char(engine, key as u32);
                if result.action == ACTION_NONE {
                    text.push(key);
                } else {
                    for _ in 0..result.backspace {
                        text.pop();
                    }
                    text.push_str(&result.output_string());
                }
            }
        }
        text
    }

    #[test]
    fn mask_decodes_letter_by_letter_and_ignores_unknown_bits() {
        assert_eq!(decode_onsets(0), ExtraOnsets::NONE);
        assert_eq!(decode_onsets(ONSET_Z), ExtraOnsets::Z);
        let all = ONSET_F | ONSET_J | ONSET_W | ONSET_Z;
        assert_eq!(decode_onsets(all), ExtraOnsets::ZFWJ);
        assert_eq!(decode_onsets(all | 0xF0), ExtraOnsets::ZFWJ);
    }

    #[test]
    fn letters_survive_configure_and_zero_turns_them_off() {
        unsafe {
            funput_set_extra_onsets(std::ptr::null_mut(), ONSET_Z);
            let engine = funput_engine_new();
            funput_set_extra_onsets(engine, ONSET_Z);
            funput_configure(
                engine,
                FunputConfig {
                    method: METHOD_TELEX,
                    tone_style: 1,
                    smart_restore: true,
                    eager_restore: true,
                    spell_check: false,
                    auto_capitalize: false,
                },
            );
            assert_eq!(typed(engine, "zoo "), "zô ");
            assert_eq!(typed(engine, "fair "), "fair ");
            funput_set_extra_onsets(engine, ONSET_Z | ONSET_F);
            assert_eq!(typed(engine, "fair "), "fải ");
            funput_set_extra_onsets(engine, 0);
            assert_eq!(typed(engine, "zoo "), "zoo ");
            funput_engine_free(engine);
        }
    }
}
