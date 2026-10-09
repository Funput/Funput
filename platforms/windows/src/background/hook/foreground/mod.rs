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

use std::sync::atomic::{AtomicIsize, Ordering};

use funput_desktop::Caret;
use windows::Win32::Foundation::HWND;
use windows::Win32::UI::Accessibility::HWINEVENTHOOK;
use windows::Win32::UI::WindowsAndMessaging::{EVENT_SYSTEM_FOREGROUND, GetForegroundWindow};

use super::{FOREGROUND_IS_FUNPUT, toggle};
use crate::background::{inject, keymap, tray};
use crate::shared::shell;

use window::{class_of_window, exe_of_window, is_shell_surface, own_exe_id};

/// The window the last foreground event was about, so a hotkey can tell whether
/// the app the shell has on record is still the one in front.
static NOTED_HWND: AtomicIsize = AtomicIsize::new(0);

/// Called by the hotkey just before it toggles: make sure the app it is about to
/// pin is the one actually in front.
///
/// The foreground event and the keystroke reach this thread by different routes,
/// and a Ctrl+Space pressed the instant after switching apps can be handled before
/// the event that announces the switch — which pinned the app the user had just
/// *left*. So when the window in front is not the one last announced, it is asked
/// directly. Two cheap calls (no file I/O), so it fits inside the keyboard hook.
pub(super) fn refresh_for_hotkey() {
    // SAFETY: Takes no arguments; a null handle is a valid answer.
    let hwnd = unsafe { GetForegroundWindow() };
    if hwnd.0 as isize == NOTED_HWND.load(Ordering::Relaxed) {
        return;
    }
    match typing_app(hwnd, &class_of_window(hwnd)) {
        Some(id) => shell::note_foreground(id),
        None => shell::clear_foreground(),
    }
}

/// The app id a hotkey pressed in `hwnd` should pin, or `None` when the window is
/// not somewhere a person types: Funput's own windows, the shell, or a window
/// whose program cannot be resolved.
fn typing_app(hwnd: HWND, class: &str) -> Option<String> {
    let id = exe_of_window(hwnd)?;
    if id == own_exe_id().as_str() || is_shell_surface(class) {
        return None;
    }
    Some(id)
}

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
    NOTED_HWND.store(hwnd.0 as isize, Ordering::Relaxed);

    // A hotkey toggle still waiting to be written down must land on disk before
    // `reload_settings` below compares the file with memory — or the reload takes
    // the unsaved toggle for a stale copy and throws it away.
    toggle::run_pending();

    // From here on, every way out that is not a regular app clears the app on
    // record, so a hotkey pressed on the desktop, the taskbar or a Funput window
    // switches VI/EN globally instead of re-pinning the app the user just left.
    let Some(id) = id else {
        shell::clear_foreground();
        return;
    };
    let is_funput = id == own_exe_id().as_str();
    FOREGROUND_IS_FUNPUT.store(is_funput, Ordering::Relaxed);
    // The caret is somewhere else entirely now, so nothing Funput has typed sits in
    // front of it any more (mirrors the mouse-click flush). Both directions: keys
    // typed into Funput's own windows compose in-process and never reach the engine.
    // Unknown rather than a line start: an app gives the caret back wherever it
    // left it, usually mid-document, so capitalizing here would be a guess.
    shell::caret_moved(Caret::Unknown);
    // Neither of these is somewhere a person is typing, so neither gets to say what
    // language they are typing in. The shell matters most: the tray icon sits on the
    // taskbar, so answering for `Shell_TrayWnd` would let a trip to Funput's own
    // flyout undo the choice the user went there to make.
    if is_funput || is_shell_surface(&class) {
        shell::clear_foreground();
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
