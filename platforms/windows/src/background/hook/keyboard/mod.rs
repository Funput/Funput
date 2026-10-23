//! The keyboard hook: what happens to every keystroke before the app sees it.
//!
//! This is the hot path. It runs inside `WH_KEYBOARD_LL`, which means the user's
//! keypress is *blocked* until it returns — so nothing here may do I/O or wait.
//!
//! Every event first goes past the gesture tracker (hotkeys made of modifiers
//! alone, and the lone Alt tap that opens an app's menus); a keydown then goes
//! on to [`keydown`], which decides whether the engine composes it.

mod keydown;

use std::sync::atomic::Ordering;

use funput_desktop::plan_inject;
use windows::Win32::Foundation::{LPARAM, LRESULT, WPARAM};
use windows::Win32::UI::Input::KeyboardAndMouse::VIRTUAL_KEY;
use windows::Win32::UI::WindowsAndMessaging::{
    CallNextHookEx, HC_ACTION, KBDLLHOOKSTRUCT, WM_KEYDOWN, WM_KEYUP, WM_SYSKEYDOWN, WM_SYSKEYUP,
};

use super::{FOREGROUND_IS_FUNPUT, access_keys, toggle};
use crate::background::hotkey::{self, Gesture, Hit};
use crate::background::{inject, keymap};
use crate::shared::shell;

pub(super) unsafe extern "system" fn keyboard_proc(
    code: i32,
    wparam: WPARAM,
    lparam: LPARAM,
) -> LRESULT {
    if code == HC_ACTION as i32 {
        // SAFETY: HC_ACTION supplies a valid KBDLLHOOKSTRUCT for this callback.
        let kbd = unsafe { &*(lparam.0 as *const KBDLLHOOKSTRUCT) };
        // Skip the events we ourselves synthesized via SendInput (no re-entrancy).
        if kbd.dwExtraInfo == shell::INJECT_TAG {
            // SAFETY: Forward the original callback arguments unchanged.
            return unsafe { CallNextHookEx(None, code, wparam, lparam) };
        }
        let msg = wparam.0 as u32;
        let down = msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN;
        if down || msg == WM_KEYUP || msg == WM_SYSKEYUP {
            let vk = VIRTUAL_KEY(kbd.vkCode as u16);
            // A modifier-only gesture resolves on release, and the tracker has to
            // see every event to know a plain key interrupted the gesture.
            // Acting on it never swallows: eating a modifier's keyup would leave
            // the focused app convinced the modifier is still down.
            match hotkey::on_key_event(vk, keymap::read_mods(), down) {
                Some(Gesture::Hotkey(hit)) => {
                    fire(hit);
                }
                Some(Gesture::Modifiers(tapped)) => access_keys::on_modifier_tap(tapped),
                None => {}
            }
        }
        if down && keydown::handle(kbd) {
            return LRESULT(1); // swallow: do not pass the key to the focused app
        }
    }
    // SAFETY: Forward the original callback arguments unchanged.
    unsafe { CallNextHookEx(None, code, wparam, lparam) }
}

/// Run the hotkey that just matched. Returns false when it was not applicable
/// after all, so the key carries on to the engine (and then to the app).
fn fire(hit: Hit) -> bool {
    // A hotkey is the user typing again, wherever the menus were.
    access_keys::cancel();
    match hit {
        // The flip must land before the next keystroke; writing it down and
        // repainting the tray must not happen here at all — see [`toggle`].
        Hit::Toggle => {
            super::foreground::refresh_for_hotkey(); // pin the app really in front
            shell::toggle_enabled_hotkey();
            toggle::defer();
            true
        }
        // Flip the word being composed VN↔raw. There is nothing to flip in
        // English mode, nor in Funput's own windows, which compose in-process.
        Hit::Flip => {
            if !shell::enabled() || FOREGROUND_IS_FUNPUT.load(Ordering::Relaxed) {
                return false;
            }
            // The hotkey's own modifiers are still held on this keydown, so the
            // Backspaces must go out with them cleared — [`inject::send_plan_unmodified`].
            let plan = plan_inject(&shell::flip_composing());
            inject::send_plan_unmodified(&plan, keymap::read_mods());
            true // swallow even on a no-op, so the hotkey never leaks to the app
        }
    }
}
