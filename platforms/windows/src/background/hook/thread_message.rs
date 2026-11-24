//! Messages posted to the hook thread itself rather than to a window.
//!
//! They have no window, so `DispatchMessageW` would drop them, and the null `hwnd`
//! is what tells each apart from a window message that happens to share its
//! number.

use windows::Win32::UI::WindowsAndMessaging::{MSG, WM_TIMER};

use super::{settings_watch, toggle};
use crate::background::tray;

/// Handle `msg` if it is one of ours; `false` leaves it for `DispatchMessageW`.
pub(super) fn handle(msg: &MSG) -> bool {
    if !msg.hwnd.0.is_null() {
        return false;
    }
    match msg.message {
        // The toggle work the hook refused to do itself.
        toggle::WM_TOGGLED => toggle::run_pending(),
        // The taskbar's scale may have changed, so the tray glyph's size too.
        tray::WM_RESCALE => tray::rescale(),
        // A thread timer. `deliver` turns down any that is not the settings watch's.
        WM_TIMER => return settings_watch::deliver(msg.wParam.0),
        _ => return false,
    }
    true
}
