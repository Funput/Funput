//! The Win32 hooks that drive the engine, and the message pump they run on.
//!
//! Three OS callbacks feed the shell, one per module here: [`keyboard`] (the
//! keystrokes themselves), [`mouse`] (a click moves the caret), and [`foreground`]
//! (the focused app changed). All three are bare `extern "system"` functions with
//! no user pointer, so they reach the engine through
//! [`crate::shared::shell`]'s process-global state.
//!
//! Everything runs on the background process's main thread, which must own a
//! message loop — `WH_KEYBOARD_LL` requires one, and the tray shares it.

mod foreground;
mod keyboard;
mod mouse;
mod toggle;

use std::sync::atomic::AtomicBool;

use windows::Win32::Foundation::HINSTANCE;
use windows::Win32::System::LibraryLoader::GetModuleHandleW;
use windows::Win32::UI::Accessibility::SetWinEventHook;
use windows::Win32::UI::WindowsAndMessaging::{
    DispatchMessageW, PeekMessageW, PostQuitMessage, SetWindowsHookExW, TranslateMessage,
    EVENT_SYSTEM_FOREGROUND, MSG, PM_REMOVE, WH_KEYBOARD_LL, WH_MOUSE_LL, WINEVENT_OUTOFCONTEXT,
    WM_QUIT,
};

use crate::background::instance::{self, WaitKind};
use crate::background::tray;
use crate::ui;

pub use toggle::set_on_toggle;

/// Whether the focused window belongs to Funput itself. Written by [`foreground`],
/// read by [`keyboard`]: our own Settings fields compose in-process, so the global
/// hook must leave their keystrokes alone.
static FOREGROUND_IS_FUNPUT: AtomicBool = AtomicBool::new(false);

/// Install the hooks and tray on the current thread, then run their Win32 message
/// pump until the tray's Quit command posts `WM_QUIT`.
pub fn run() {
    unsafe {
        let hmod = GetModuleHandleW(None).unwrap_or_default();
        let hook = SetWindowsHookExW(
            WH_KEYBOARD_LL,
            Some(keyboard::keyboard_proc),
            Some(HINSTANCE(hmod.0)),
            0,
        );
        if hook.is_err() {
            eprintln!("Funput: failed to install keyboard hook: {hook:?}");
            return;
        }

        // Also watch the mouse: a click repositions the caret, which the keyboard
        // hook can't see, so composition must be flushed (mirrors the Enter/arrow
        // `KeyKind::Flush` path) — otherwise the next keystroke diffs against a stale
        // word at the new caret and corrupts neighbouring text. Delivered to this
        // thread's message pump, so it shares the engine via `shell` like the others.
        let mouse_hook = SetWindowsHookExW(
            WH_MOUSE_LL,
            Some(mouse::mouse_proc),
            Some(HINSTANCE(hmod.0)),
            0,
        );
        if mouse_hook.is_err() {
            // Non-fatal: typing still works, only mouse-click flush is unavailable.
            eprintln!("Funput: failed to install mouse hook: {mouse_hook:?}");
        }
        // Also watch foreground changes for per-app VI/EN auto-switch. An OUT_OF_CONTEXT
        // WinEvent hook is delivered to this thread's message queue (same pump below),
        // so its callback shares the engine via `shell` with no extra synchronization.
        let _win_event = SetWinEventHook(
            EVENT_SYSTEM_FOREGROUND,
            EVENT_SYSTEM_FOREGROUND,
            None, // OUT_OF_CONTEXT: no DLL module
            Some(foreground::win_event_proc),
            0,
            0,
            WINEVENT_OUTOFCONTEXT,
        );

        // The tray lives on this thread too, so its menu/click messages flow through
        // the same pump and its events can be drained right after each dispatch.
        tray::install();

        // Wait on the activate Event, the current UI child, and the thread queue
        // together: a second Funput.exe launch must open Settings while the pump is
        // idle, and the Control Center reports the button the user pressed through
        // its exit code, which reaches us only by waiting on its handle.
        let mut msg = MSG::default();
        loop {
            // Re-read every turn: reaping one child can spawn the next (flyout →
            // Settings), so the handle to wait on changes as we go.
            let child = ui::current_child_handle();
            match instance::wait_activate_message_or_child(child) {
                WaitKind::Activate => {
                    ui::launch_settings(false);
                }
                WaitKind::ChildExited => {
                    ui::reap_ui_child();
                }
                WaitKind::Message => {
                    while PeekMessageW(&mut msg, None, 0, 0, PM_REMOVE).as_bool() {
                        if msg.message == WM_QUIT {
                            return;
                        }
                        // The toggle work the hook refused to do itself. Posted to
                        // the thread, so it has no window and dispatching would
                        // drop it — and the null `hwnd` is what tells it apart
                        // from a window message that happens to share the number.
                        if msg.message == toggle::WM_TOGGLED && msg.hwnd.0.is_null() {
                            toggle::run_pending();
                            continue;
                        }
                        let _ = TranslateMessage(&msg);
                        DispatchMessageW(&msg);
                        tray::drain_events();
                    }
                }
                WaitKind::Failed => return,
            }
        }
    }
}

/// Stop the background message loop. Called on the hook/tray thread.
pub fn quit() {
    unsafe { PostQuitMessage(0) };
}
