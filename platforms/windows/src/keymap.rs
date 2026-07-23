//! Translate a Windows low-level keyboard event into the host-neutral
//! [`funput_desktop::KeyEvent`] the classifier understands.

use funput_desktop::{KeyEvent, KeySource, Mods};
use windows::Win32::UI::Input::KeyboardAndMouse::{
    GetAsyncKeyState, GetKeyState, GetKeyboardLayout, ToUnicodeEx, VIRTUAL_KEY, VK_BACK,
    VK_CAPITAL, VK_CONTROL, VK_DELETE, VK_DOWN, VK_END, VK_ESCAPE, VK_HOME, VK_INSERT, VK_LCONTROL,
    VK_LEFT, VK_LMENU, VK_LSHIFT, VK_LWIN, VK_MENU, VK_NEXT, VK_OEM_3, VK_PRIOR, VK_RCONTROL,
    VK_RETURN, VK_RIGHT, VK_RMENU, VK_RSHIFT, VK_RWIN, VK_SHIFT, VK_SPACE, VK_TAB, VK_UP,
};
use windows::Win32::UI::WindowsAndMessaging::KBDLLHOOKSTRUCT;

use crate::settings::{FlipHotkey, Hotkey, KeyCombo};

fn async_down(vk: VIRTUAL_KEY) -> bool {
    (unsafe { GetAsyncKeyState(vk.0 as i32) } as u16 & 0x8000) != 0
}

/// Physical modifier state. Shift is reported but is not a "shortcut" by itself.
pub fn read_mods() -> Mods {
    Mods {
        ctrl: async_down(VK_CONTROL),
        alt: async_down(VK_MENU),
        win: async_down(VK_LWIN) || async_down(VK_RWIN),
        shift: async_down(VK_SHIFT),
    }
}

/// Fold side-specific modifier virtual-keys back to their generic codes. The
/// low-level hook always delivers the side-specific keys (`VK_LSHIFT`,
/// `VK_RMENU`, …), never the generic `VK_SHIFT`/`VK_MENU`/`VK_CONTROL` an app
/// sees after message translation — comparing against the generics without this
/// fold can never match (the original Alt+Shift preset bug).
fn normalize_vk(vk: VIRTUAL_KEY) -> VIRTUAL_KEY {
    match vk {
        VK_LSHIFT | VK_RSHIFT => VK_SHIFT,
        VK_LCONTROL | VK_RCONTROL => VK_CONTROL,
        VK_LMENU | VK_RMENU => VK_MENU,
        VK_RWIN => VK_LWIN,
        other => other,
    }
}

/// Whether this keydown matches the configured VI/EN toggle hotkey. Presets
/// match their exact modifier set, so nearby combos (e.g. Ctrl+Shift+Space)
/// still reach the focused app.
pub fn is_toggle(vk: VIRTUAL_KEY, mods: Mods, hotkey: Hotkey) -> bool {
    let vk = normalize_vk(vk);
    match hotkey {
        Hotkey::CtrlBacktick => {
            mods.ctrl && !mods.alt && !mods.win && !mods.shift && vk == VK_OEM_3
        }
        Hotkey::CtrlSpace => mods.ctrl && !mods.alt && !mods.win && !mods.shift && vk == VK_SPACE,
        // Modifier-only combo: fires on the second modifier's keydown. The async
        // key state may not yet include the key this very event is delivering,
        // so the pressed key itself also counts as held.
        Hotkey::AltShift => {
            let alt = mods.alt || vk == VK_MENU;
            let shift = mods.shift || vk == VK_SHIFT;
            alt && shift && !mods.ctrl && !mods.win && (vk == VK_SHIFT || vk == VK_MENU)
        }
    }
}

/// Whether this keydown matches a user-recorded combo: the main key plus the
/// *exact* modifier set (a combo's main key is never a modifier, so the async
/// modifier state is already settled by the time it fires).
pub fn matches_combo(vk: VIRTUAL_KEY, mods: Mods, combo: &KeyCombo) -> bool {
    normalize_vk(vk).0 == combo.vk
        && mods.ctrl == combo.ctrl
        && mods.alt == combo.alt
        && mods.shift == combo.shift
        && mods.win == combo.win
}

/// Whether this keydown matches the configured flip hotkey. Letter virtual-keys are
/// their ASCII uppercase codes (`Z` = 0x5A, `X` = 0x58).
pub fn is_flip(vk: VIRTUAL_KEY, mods: Mods, hotkey: FlipHotkey) -> bool {
    let vk = normalize_vk(vk);
    let ctrl_shift = mods.ctrl && mods.shift && !mods.alt && !mods.win;
    match hotkey {
        FlipHotkey::Off => false,
        FlipHotkey::CtrlShiftZ => ctrl_shift && vk.0 == 0x5A,
        FlipHotkey::CtrlShiftX => ctrl_shift && vk.0 == 0x58,
    }
}

fn is_navigation(vk: VIRTUAL_KEY) -> bool {
    if matches!(
        vk,
        VK_RETURN
            | VK_TAB
            | VK_ESCAPE
            | VK_LEFT
            | VK_RIGHT
            | VK_UP
            | VK_DOWN
            | VK_HOME
            | VK_END
            | VK_PRIOR
            | VK_NEXT
            | VK_DELETE
            | VK_INSERT
    ) {
        return true;
    }
    // F1..F24 (0x70..=0x87).
    (0x70..=0x87).contains(&vk.0)
}

/// The character this key would produce, ignoring Ctrl/Alt (those keys never
/// compose). Shift and CapsLock are honored so uppercase letters reach the engine.
fn translate_char(kbd: &KBDLLHOOKSTRUCT) -> Option<char> {
    let mut state = [0u8; 256];
    if async_down(VK_SHIFT) {
        state[VK_SHIFT.0 as usize] = 0x80;
    }
    if (unsafe { GetKeyState(VK_CAPITAL.0 as i32) } & 0x0001) != 0 {
        state[VK_CAPITAL.0 as usize] = 0x01;
    }
    let layout = unsafe { GetKeyboardLayout(0) };
    let mut buf = [0u16; 8];
    let n = unsafe { ToUnicodeEx(kbd.vkCode, kbd.scanCode, &state, &mut buf, 0, Some(layout)) };
    if n == 1 {
        char::from_u32(buf[0] as u32)
    } else {
        None // dead key (<0), no mapping (0), or multi-char (>1): not composable
    }
}

/// Where the key physically sits. Numpad digits report `VK_NUMPAD0..=VK_NUMPAD9`
/// (0x60..=0x69) — only while NumLock is on, which is exactly when they produce a
/// digit — so the engine can keep them literal numbers instead of VNI modifiers.
/// (NumLock off makes those keys arrows/Home/End, handled as navigation upstream.)
fn key_source(vk: VIRTUAL_KEY) -> KeySource {
    if (0x60..=0x69).contains(&vk.0) {
        KeySource::Numpad
    } else {
        KeySource::Standard
    }
}

pub fn to_key_event(kbd: &KBDLLHOOKSTRUCT) -> KeyEvent {
    let vk = VIRTUAL_KEY(kbd.vkCode as u16);
    KeyEvent {
        mods: read_mods(),
        ch: translate_char(kbd),
        is_backspace: vk == VK_BACK,
        is_navigation: is_navigation(vk),
        source: key_source(vk),
    }
}

#[cfg(test)]
mod tests;
