//! The slow half of a VI/EN hotkey toggle, kept out of the keyboard hook.
//!
//! Persisting the settings is a file write, and refreshing the tray is two
//! cross-process calls into Explorer; neither has an upper bound on how long it
//! takes. Windows silently removes a `WH_KEYBOARD_LL` hook whose callback
//! overruns `LowLevelHooksTimeout` (300 ms by default), and nothing reinstalls
//! it — Funput would go deaf mid-session with no hotkey, no composition, and no
//! error anywhere. So the hook flips the in-memory state and posts here; the
//! pump in [`super::run`] does the rest one message later, with the user's
//! keystroke already delivered.

use std::sync::OnceLock;
use std::sync::atomic::{AtomicBool, Ordering};

use windows::Win32::Foundation::{LPARAM, WPARAM};
use windows::Win32::System::Threading::GetCurrentThreadId;
use windows::Win32::UI::WindowsAndMessaging::{PostThreadMessageW, WM_APP};

use crate::shared::shell;

/// Posted by [`defer`] to the hook thread, recognized by [`super::run`]'s pump.
pub(super) const WM_TOGGLED: u32 = WM_APP + 1;

/// Called after a toggle so the tray can refresh its icon and tooltip.
type ToggleCb = Box<dyn Fn(bool) + Send + Sync>;
static ON_TOGGLE: OnceLock<ToggleCb> = OnceLock::new();

/// Whether a toggle is still waiting to be written down. A flag rather than a
/// count: holding Ctrl and tapping Shift ten times leaves one write to do, not
/// ten, and the last state is the only one worth saving.
static PENDING: AtomicBool = AtomicBool::new(false);

/// The same, for a VI/EN change that has nothing to write down — the foreign-layout
/// auto-switch, which lives in memory only. Separate from [`PENDING`] so it cannot
/// swallow a real toggle's file write, or add one the layout rule never wanted.
static PENDING_NOTIFY: AtomicBool = AtomicBool::new(false);

pub fn set_on_toggle(f: impl Fn(bool) + Send + Sync + 'static) {
    let _ = ON_TOGGLE.set(Box::new(f));
}

/// Hand the toggle's side effects to the pump. Called from inside the hook, so
/// it must not block — posting a message never waits on the receiver.
pub(super) fn defer() {
    PENDING.store(true, Ordering::Relaxed);
    post();
}

/// Refresh the tray after a VI/EN change that is not the user's stored choice, so
/// nothing is persisted. Same reason for deferring: repainting the tray is two
/// cross-process calls into Explorer, and this is called from inside the hook.
pub(super) fn defer_notify() {
    PENDING_NOTIFY.store(true, Ordering::Relaxed);
    post();
}

fn post() {
    unsafe {
        let _ = PostThreadMessageW(GetCurrentThreadId(), WM_TOGGLED, WPARAM(0), LPARAM(0));
    }
}

/// Run those side effects, on the pump thread and off the hook.
pub(super) fn run_pending() {
    // Both are taken every time: a toggle and a layout switch can land between two
    // messages, and leaving either flag standing would spend a later message on it.
    let toggled = PENDING.swap(false, Ordering::Relaxed);
    let notified = PENDING_NOTIFY.swap(false, Ordering::Relaxed);
    if !toggled && !notified {
        return; // a burst of changes: an earlier message already covered this one
    }
    if toggled {
        shell::save_settings();
    }
    // Read the state fresh rather than carrying it in the message: by now several
    // may have landed, and the tray only ever wants the current one.
    notify(shell::enabled());
}

/// Tell the tray about a VI/EN change that did not come from the hotkey — the
/// per-app auto-switch, or a config reload. Callable directly because those all
/// arrive on the pump, where taking a moment costs nobody a keystroke.
pub(super) fn notify(on: bool) {
    if let Some(cb) = ON_TOGGLE.get() {
        cb(on);
    }
}
