//! Translate a Windows low-level keyboard event into the host-neutral
//! [`funput_desktop::KeyEvent`] the classifier understands.

use funput_desktop::{KeyEvent, KeySource, Mods};
use windows::Win32::UI::Input::KeyboardAndMouse::{
    GetAsyncKeyState, GetKeyState, GetKeyboardLayout, ToUnicodeEx, VIRTUAL_KEY, VK_BACK,
    VK_CAPITAL, VK_CONTROL, VK_DELETE, VK_DOWN, VK_END, VK_ESCAPE, VK_HOME, VK_INSERT, VK_LEFT,
    VK_LWIN, VK_MENU, VK_NEXT, VK_PRIOR, VK_RETURN, VK_RIGHT, VK_RWIN, VK_SHIFT, VK_TAB, VK_UP,
};
use windows::Win32::UI::WindowsAndMessaging::{
    GetForegroundWindow, GetWindowThreadProcessId, KBDLLHOOKSTRUCT,
};

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

/// The keyboard layout handle of whatever has focus, as a plain `u32` for
/// [`funput_desktop::is_foreign_layout`]. Zero when there is no foreground window.
///
/// The input language is a property of the *thread* that owns the window, which is
/// what makes this the right question to ask however the user has Windows set up:
/// one language for everything, or one per app window. `GetKeyboardLayout(0)`
/// above answers for Funput's own hook thread instead, which is a different
/// question and never changes.
///
/// Three cheap user32 calls that read session state — no cross-process work, no
/// blocking — which is what makes this safe to do from inside the hook. Windows
/// has no global notification for an input-language change (`WM_INPUTLANGCHANGE`
/// goes only to the app that owns the change, and the TSF sinks are per-thread),
/// so asking on each keystroke is the only way to know; and asking *there* is also
/// what guarantees the first key typed after Win+Space is already judged right.
pub fn foreground_layout() -> u32 {
    unsafe {
        let hwnd = GetForegroundWindow();
        if hwnd.0.is_null() {
            return 0;
        }
        let tid = GetWindowThreadProcessId(hwnd, None);
        GetKeyboardLayout(tid).0 as usize as u32
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
