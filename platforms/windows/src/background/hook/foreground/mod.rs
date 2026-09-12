//! The foreground-window hook: the user switched apps.
//!
//! Two things follow from that. The caret is somewhere else entirely, so anything
//! Funput was composing is stale; and the new app may want a different VI/EN state
//! (see [`crate::shared::shell`]'s per-app auto-switch).
//!
//! Twice over, though, a foreground change is **not** a user switching apps: when
//! the window is one of Funput's own, and when it is the shell itself — the taskbar,
//! the desktop, Alt-Tab. Both are turned away here, and [`window`] is where a window
//! is asked which it is.

mod window;

use std::sync::atomic::Ordering;

use windows::Win32::Foundation::HWND;
use windows::Win32::UI::Accessibility::HWINEVENTHOOK;
use windows::Win32::UI::WindowsAndMessaging::EVENT_SYSTEM_FOREGROUND;

use super::{FOREGROUND_IS_FUNPUT, toggle};
use crate::background::{inject, keymap, tray};
use crate::shared::shell;

use window::{class_of_window, exe_of_window, is_shell_surface, own_exe_id};

/// Foreground-window changed: record the app and apply its per-app VI/EN default.
pub(super) unsafe extern "system" fn win_event_proc(
    _hook: HWINEVENTHOOK,
    event: u32,
    hwnd: HWND,
    _id_object: i32,
    _id_child: i32,
    _thread: u32,
    _time: u32,
) {
    if event != EVENT_SYSTEM_FOREGROUND {
        return;
    }
    // Whether a replacement sent to this app needs the lead character that makes its
    // Backspaces unambiguous, or would only be hurt by the extra Backspace the lead
    // costs — see `inject::send_plan`. Decided from the window's class first, so a
    // window whose process cannot be resolved still updates it rather than leaving
    // the previous app's answer standing.
    let class = class_of_window(hwnd);
    let id = exe_of_window(hwnd);
    inject::note_foreground(&class, id.as_deref().unwrap_or_default());

    let Some(id) = id else {
        return;
    };
    let is_funput = id == own_exe_id().as_str();
    FOREGROUND_IS_FUNPUT.store(is_funput, Ordering::Relaxed);
    // The caret is somewhere else entirely now, so nothing Funput has typed sits in
    // front of it any more (mirrors the mouse-click flush). Both directions: keys
    // typed into Funput's own windows compose in-process and never reach the engine.
    shell::clear();
    // Neither of these is somewhere a person is typing, so neither gets to say what
    // language they are typing in. The shell matters most: the tray icon sits on the
    // taskbar, so answering for `Shell_TrayWnd` would let a trip to Funput's own
    // flyout undo the choice the user went there to make.
    if is_funput || is_shell_surface(&class) {
        return;
    }

    // A Settings child persists changes to disk. Reload them as soon as focus
    // returns to a regular app, before the next keystroke reaches the engine. A
    // VI/EN flip made there arrives as the global default — `apply_for_app` below
    // can still overrule it, but only for an app the user pinned with the hotkey.
    if shell::reload_settings() {
        tray::sync_from_shell();
    }
    shell::note_foreground(id.clone());
    // Focus on a new app is the start of input: arm so the first letter is capitalized.
    shell::arm_capitalization();
    let by_app = shell::apply_for_app(&id);
    // The layout rule gets the last word, so an app remembered as Vietnamese does
    // not turn it back on inside an app whose thread is running a Japanese IME.
    // Input language is per-thread, so changing app can change it with no keystroke
    // in between — this is the other place it has to be asked.
    let by_layout = shell::apply_for_layout(keymap::foreground_layout());
    if by_app.is_some() || by_layout.is_some() {
        // Read back rather than trusting either answer: when both fired, only the
        // second one is still true. Keeps the tray icon and tooltip in sync.
        toggle::notify(shell::enabled());
    }
}
