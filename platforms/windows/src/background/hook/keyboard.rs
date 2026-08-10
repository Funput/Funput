//! The keyboard hook: what happens to every keystroke before the app sees it.
//!
//! This is the hot path. It runs inside `WH_KEYBOARD_LL`, which means the user's
//! keypress is *blocked* until it returns — so nothing here may do I/O or wait.

use std::sync::atomic::Ordering;

use funput_desktop::{classify, plan_inject, KeyKind};
use windows::Win32::Foundation::{LPARAM, LRESULT, WPARAM};
use windows::Win32::UI::Input::KeyboardAndMouse::{VIRTUAL_KEY, VK_RETURN};
use windows::Win32::UI::WindowsAndMessaging::{
    CallNextHookEx, HC_ACTION, KBDLLHOOKSTRUCT, WM_KEYDOWN, WM_KEYUP, WM_SYSKEYDOWN, WM_SYSKEYUP,
};

use super::{toggle, FOREGROUND_IS_FUNPUT};
use crate::background::hotkey::{self, Hit};
use crate::background::{inject, keymap};
use crate::shared::shell;

pub(super) unsafe extern "system" fn keyboard_proc(
    code: i32,
    wparam: WPARAM,
    lparam: LPARAM,
) -> LRESULT {
    if code == HC_ACTION as i32 {
        let kbd = &*(lparam.0 as *const KBDLLHOOKSTRUCT);
        // Skip the events we ourselves synthesized via SendInput (no re-entrancy).
        if kbd.dwExtraInfo == shell::INJECT_TAG {
            return CallNextHookEx(None, code, wparam, lparam);
        }
        let msg = wparam.0 as u32;
        let down = msg == WM_KEYDOWN || msg == WM_SYSKEYDOWN;
        if down || msg == WM_KEYUP || msg == WM_SYSKEYUP {
            let vk = VIRTUAL_KEY(kbd.vkCode as u16);
            // A modifier-only hotkey resolves on release, and the tracker has to
            // see every event to know a plain key interrupted the gesture.
            // Firing never swallows: eating a modifier's keyup would leave the
            // focused app convinced the modifier is still down.
            if let Some(hit) = hotkey::on_key_event(vk, keymap::read_mods(), down) {
                fire(hit);
            }
        }
        if down && handle_keydown(kbd) {
            return LRESULT(1); // swallow: do not pass the key to the focused app
        }
    }
    CallNextHookEx(None, code, wparam, lparam)
}

/// Run the hotkey that just matched. Returns false when it was not applicable
/// after all, so the key carries on to the engine (and then to the app).
fn fire(hit: Hit) -> bool {
    match hit {
        // The flip must land before the next keystroke; writing it down and
        // repainting the tray must not happen here at all — see [`toggle`].
        Hit::Toggle => {
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
            inject::send_plan_unmodified(
                &plan,
                keymap::read_mods(),
                shell::foreground_has_urlbar_autofill(),
            );
            true // swallow even on a no-op, so the hotkey never leaks to the app
        }
    }
}

/// Returns true if the key should be swallowed (we injected a replacement), false
/// to let it reach the app.
fn handle_keydown(kbd: &KBDLLHOOKSTRUCT) -> bool {
    let vk = VIRTUAL_KEY(kbd.vkCode as u16);
    let mods = keymap::read_mods();

    if let Some(hit) = hotkey::on_keydown(vk, mods) {
        if fire(hit) {
            return true;
        }
    }

    if !shell::enabled() {
        return false; // English mode: hands off
    }

    // Settings/Onboarding run in a separate Funput process. Their fields compose
    // in-process, so the background hook must leave their keystrokes untouched.
    if FOREGROUND_IS_FUNPUT.load(Ordering::Relaxed) {
        return false;
    }

    // A modifier going *down* is not a shortcut being run; which shortcut it is
    // only becomes known when the main key lands. `classify` saw the already-held
    // Ctrl and flushed, so Ctrl+Shift+Z destroyed the composing word on its own
    // Shift keydown — one event before flip could act on it. Only the low-level
    // hook reaches here: macOS sends modifier changes as `flagsChanged`, not keys.
    if hotkey::is_modifier_key(vk) {
        return false;
    }

    match classify(&keymap::to_key_event(kbd)) {
        KeyKind::Compose(c, source) => {
            let plan = plan_inject(&shell::process_key(c, source));
            if plan.is_noop() {
                false // Action::None — the literal key reaches the app
            } else {
                inject::send_plan_auto(&plan); // delete + retype the composed text
                true
            }
        }
        KeyKind::Backspace => {
            // Sync Funput's model of the text; the app deletes its own char. When the
            // deletion puts the caret back on a finished word, this re-opens it so the
            // next keystroke retones it (`phủ` + Space + ⌫ + `s` → `phú`).
            shell::on_backspace();
            false
        }
        KeyKind::Flush => {
            shell::clear(); // commit what is shown; nav/Enter/Tab/shortcut passes
                            // Enter starts a new line → arm auto-capitalize (no-op unless the feature
                            // is on). The engine never sees the newline itself on this path.
            if vk == VK_RETURN {
                shell::arm_capitalization();
            }
            false
        }
        KeyKind::PassThrough => false,
    }
}
