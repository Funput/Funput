//! Text-expansion shortcut (gõ tắt) table sync over the C ABI.

use super::FunputEngine;
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
