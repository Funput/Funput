//! Text-expansion shortcut (gõ tắt) table sync over the C ABI.

use std::ffi::c_void;

use super::{FunputEngine, FunputResult, compose::decode_source};
use crate::abi;

/// Define a text-expansion shortcut (gõ tắt): typing `trigger` then a word boundary
/// injects `expansion` (`vn` → `Việt Nam`). Both strings are passed as UTF-32
/// (`*const u32` + length), matching [`crate::funput_buffer`]. An empty trigger is
/// ignored by the engine; re-adding a trigger overwrites it.
///
/// Hosts sync the whole table by calling [`funput_clear_shortcuts`] then adding each
/// entry (the engine is a runtime mirror of the host's config).
///
/// Null-safe: a null handle does nothing. A null `trigger`/`expansion` pointer is
/// treated as an empty string.
///
/// # Safety
/// `engine` must be a valid handle or null. `trigger` must point to at least
/// `trigger_len` `u32` values (or be null), and `expansion` to at least
/// `expansion_len` `u32` values (or be null).
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_add_shortcut(
    engine: *mut FunputEngine,
    trigger: *const u32,
    trigger_len: usize,
    expansion: *const u32,
    expansion_len: usize,
) {
    unsafe {
        abi::with_engine_mut(engine, |e| {
            let trigger = abi::string_from_utf32(trigger, trigger_len);
            let expansion = abi::string_from_utf32(expansion, expansion_len);
            e.add_shortcut(trigger, expansion);
        })
    }
}

/// Remove every text-expansion shortcut. Combine with [`funput_add_shortcut`] to
/// replace the whole table when syncing from config.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_clear_shortcuts(engine: *mut FunputEngine) {
    unsafe { abi::with_engine_mut(engine, |e| e.clear_shortcuts()) }
}

/// Turn gõ tắt expansion on or off ("Bật gõ tắt"). Off, a trigger is typed out as
/// itself and nothing expands; the table is left loaded, so the switch is instant in
/// both directions and hosts need not re-push their rows around it.
///
/// Its own function rather than a [`crate::FunputConfig`] field, for the ABI reason
/// documented there; [`crate::funput_configure`] leaves this setting alone, so the
/// two may be called in either order.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_shortcuts_enabled(engine: *mut FunputEngine, on: bool) {
    unsafe { abi::with_engine_mut(engine, |e| e.update_config(|c| c.shortcuts_enabled = on)) }
}

/// Turn smart-case matching on or off ("Tự nhận diện hoa/thường"). On (the default),
/// a trigger typed lowercase, Title Case, or UPPERCASE all resolve to the same entry
/// and the expansion is re-cased to match (`tp`/`Tp`/`TP` → `TP. HCM`/`Tp. Hcm`/
/// `TP. HCM`). Off, only the exact trigger expands and the expansion comes out
/// verbatim.
///
/// Its own function rather than a [`crate::FunputConfig`] field, for the ABI reason
/// documented there; [`crate::funput_configure`] leaves this setting alone, so the
/// two may be called in either order.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_shortcut_smart_case(engine: *mut FunputEngine, on: bool) {
    unsafe { abi::with_engine_mut(engine, |e| e.update_config(|c| c.shortcut_smart_case = on)) }
}

/// Allow gõ tắt in English mode. Defaults to on; the master shortcut switch
/// and a non-empty table are still required. `funput_configure` preserves it.
///
/// # Safety
/// `engine` must be a valid handle or null.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_set_shortcuts_in_english(engine: *mut FunputEngine, on: bool) {
    unsafe { abi::with_engine_mut(engine, |e| e.update_config(|c| c.shortcuts_in_english = on)) }
}

/// Process a key like `funput_process_key`, delivering the complete output as UTF-8
/// to `receive` synchronously before returning. The returned POD remains compatible
/// with `FunputResult` (its inline chars can still truncate at `CHARS_CAP`).
/// The callback's text is borrowed only for the duration of that callback. It must
/// not re-enter or free the engine. Null callback, null engine or invalid codepoint
/// returns a no-op without processing a key.
///
/// # Safety
/// `engine` must be a valid handle or null. `receive` must accept `context` and a
/// borrowed UTF-8 pointer/length, and must not unwind across the C boundary.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn funput_process_key_text(
    engine: *mut FunputEngine,
    codepoint: u32,
    source: u32,
    receive: Option<unsafe extern "C" fn(*mut c_void, *const u8, usize)>,
    context: *mut c_void,
) -> FunputResult {
    unsafe {
        abi::with_engine_mut(engine, |e| {
            let (Some(key), Some(receive)) = (char::from_u32(codepoint), receive) else {
                return FunputResult::none();
            };
            let result = e.process_key(key, decode_source(source));
            receive(context, result.output.as_ptr(), result.output.len());
            FunputResult::from_ime(&result)
        })
    }
}

#[cfg(test)]
mod tests {
    use crate::*;

    #[test]
    fn english_switch_survives_configure_and_leaves_vietnamese_shortcuts_on() {
        unsafe {
            funput_set_shortcuts_in_english(std::ptr::null_mut(), false);
            let engine = funput_engine_new();
            let trigger = ['v' as u32, 'n' as u32];
            let expansion = ['V' as u32, 'N' as u32];
            funput_add_shortcut(engine, trigger.as_ptr(), 2, expansion.as_ptr(), 2);
            funput_set_enabled(engine, false);
            for on in [false, true] {
                funput_clear(engine);
                funput_set_shortcuts_in_english(engine, on);
                funput_configure(
                    engine,
                    FunputConfig {
                        method: METHOD_TELEX,
                        tone_style: 1,
                        smart_restore: true,
                        eager_restore: true,
                        spell_check: false,
                        auto_capitalize: true,
                    },
                );
                for key in trigger {
                    assert_eq!(funput_process_char(engine, key).action, ACTION_NONE);
                }
                let result = funput_process_char(engine, ' ' as u32);
                assert_eq!(result.action, if on { ACTION_SEND } else { ACTION_NONE });
                if on {
                    assert_eq!(result.output_string(), "VN ");
                }
            }
            funput_set_shortcuts_in_english(engine, false);
            funput_set_enabled(engine, true);
            for key in trigger {
                funput_process_char(engine, key);
            }
            assert_eq!(funput_process_char(engine, ' ' as u32).action, ACTION_SEND);
            funput_engine_free(engine);
        }
    }
}

#[cfg(test)]
mod text_tests {
    use super::*;
    use crate::*;

    unsafe extern "C" fn collect(context: *mut c_void, bytes: *const u8, len: usize) {
        unsafe {
            let output = &mut *context.cast::<String>();
            *output = String::from_utf8(std::slice::from_raw_parts(bytes, len).to_vec()).unwrap();
        }
    }

    #[test]
    fn complete_output_and_null_inputs() {
        unsafe {
            let engine = funput_engine_new();
            funput_set_enabled(engine, false);
            let expansion = "địa chỉ ".repeat(100);
            let chars: Vec<u32> = expansion.chars().map(u32::from).collect();
            funput_add_shortcut(
                engine,
                ['v' as u32].as_ptr(),
                1,
                chars.as_ptr(),
                chars.len(),
            );
            let mut output = String::new();
            let context = (&mut output as *mut String).cast();
            assert_eq!(
                funput_process_key_text(engine, 0x110000, 0, Some(collect), context).action,
                ACTION_NONE
            );
            assert_eq!(
                funput_process_key_text(engine, 'v' as u32, 0, None, context).action,
                ACTION_NONE
            );
            assert_eq!(
                funput_process_key_text(std::ptr::null_mut(), 32, 0, Some(collect), context).action,
                ACTION_NONE
            );
            funput_process_key_text(engine, 'v' as u32, 0, Some(collect), context);
            assert!(output.is_empty());
            let result = funput_process_key_text(engine, 32, 0, Some(collect), context);
            assert_eq!(result.action, ACTION_SEND);
            assert_eq!(result.backspace, 1);
            assert_eq!(output, format!("{expansion} "));
            funput_engine_free(engine);
        }
    }
}
