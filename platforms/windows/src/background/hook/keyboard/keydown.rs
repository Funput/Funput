//! One keydown, from the hotkeys down to the engine: whether it is composed, and
//! what goes out in its place when it is.

use std::sync::atomic::Ordering;

use funput_desktop::{ImeResult, KeyKind, classify, plan_inject};
use windows::Win32::UI::Input::KeyboardAndMouse::VIRTUAL_KEY;
use windows::Win32::UI::WindowsAndMessaging::KBDLLHOOKSTRUCT;

use super::super::{FOREGROUND_IS_FUNPUT, access_keys, toggle};
use super::fire;
use crate::background::{hotkey, inject, keymap};
use crate::shared::shell;

/// Returns true if the key should be swallowed (we injected a replacement), false
/// to let it reach the app.
pub(super) fn handle(kbd: &KBDLLHOOKSTRUCT) -> bool {
    let vk = VIRTUAL_KEY(kbd.vkCode as u16);
    let mods = keymap::read_mods();

    // Before anything decides what to do with this key: the user may have changed
    // input language since the last one (Win+Space, Alt+Shift), and Vietnamese
    // cannot be typed into a Japanese IME. Windows offers no notification for that,
    // so it is asked here — where the answer lands one key *ahead* of the first
    // keystroke it would affect. Funput's own windows are excluded so the state
    // stays put while Settings has focus.
    if !FOREGROUND_IS_FUNPUT.load(Ordering::Relaxed)
        && shell::apply_for_layout(keymap::foreground_layout()).is_some()
    {
        toggle::defer_notify();
    }

    if let Some(hit) = hotkey::on_keydown(vk, mods)
        && fire(hit)
    {
        return true;
    }

    if !shell::hook_active() {
        return false; // English mode with no gõ tắt to do: hands off
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

    let kind = classify(&keymap::to_key_event(kbd));
    // After a lone Alt tap the keys are menu accelerators (Excel's Alt, E, S, V):
    // they reach the app as typed, and the engine never hears of them.
    if access_keys::bypasses(&kind) {
        return false;
    }
    dispatch(kind)
}

/// Act on a key the engine is meant to see. Returns true when it was swallowed.
fn dispatch(kind: KeyKind) -> bool {
    match kind {
        KeyKind::Compose(c, source) => inject_result(&shell::process_key(c, source)),
        // Right after a gõ tắt expansion, Backspace takes it back: the shell hands
        // over the delete + retype and the key is swallowed. Otherwise it only syncs
        // Funput's model and the app deletes its own char; when that puts the caret
        // back on a finished word, the word is re-opened so the next keystroke
        // retones it (`phủ` + Space + ⌫ + `s` → `phú`).
        KeyKind::Backspace => shell::on_backspace().is_some_and(|undo| inject_result(&undo)),
        KeyKind::Flush(caret) => {
            // Nav/Enter/Tab/shortcut: commit what is shown and let the key pass.
            // `classify` has said where it leaves the caret — a line start for
            // Enter, whose newline the engine never sees on this path.
            shell::caret_moved(caret);
            false
        }
        KeyKind::PassThrough => false,
    }
}

/// Inject what the engine decided in place of the key. `false` (`Action::None`)
/// lets the literal key reach the app; an empty plan still swallows it, because
/// the engine took the key without changing what is on screen.
fn inject_result(result: &ImeResult) -> bool {
    let Some(plan) = plan_inject(result) else {
        return false;
    };
    inject::send_plan(&plan);
    true
}
