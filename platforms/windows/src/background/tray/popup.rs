//! Shows the right-click menu clear of the taskbar instead of on top of it.
//!
//! `tray-icon` can open the menu itself, but it opens it at the cursor — and the
//! cursor is on the taskbar, so part of the menu always lies over it. That is fine
//! only while the menu is the upper window. Windows 11 sometimes raises the taskbar
//! above an open popup menu (after Quick Settings has been used, or when the pointer
//! crosses a taskbar button), and whatever overlaps is then hidden. The z-order is
//! Windows' to decide; where the menu sits is not, so it is anchored to the edge of
//! the work area and nothing is left over the taskbar to hide.

use tray_icon::{MouseButton, MouseButtonState, TrayIconEvent};
use windows::Win32::Foundation::{HWND, LPARAM, POINT, RECT, WPARAM};
use windows::Win32::UI::WindowsAndMessaging::{
    HMENU, PostMessageW, SetForegroundWindow, TPM_BOTTOMALIGN, TPM_LEFTALIGN, TPM_RIGHTALIGN,
    TPM_TOPALIGN, TRACK_POPUP_MENU_FLAGS, TrackPopupMenu, WM_NULL,
};

use crate::shared::monitor;

/// Where the pointer was, in physical screen pixels, if `ev` is the right-button
/// release that asks for the menu.
pub(super) fn right_click(ev: &TrayIconEvent) -> Option<POINT> {
    match *ev {
        TrayIconEvent::Click {
            button: MouseButton::Right,
            button_state: MouseButtonState::Up,
            position,
            ..
        } => Some(POINT {
            x: position.x as i32,
            y: position.y as i32,
        }),
        _ => None,
    }
}

/// Open `menu`, owned by the tray's hidden window, for a click at `cursor`.
///
/// Returns once the menu closes: `TrackPopupMenu` runs its own message loop. The
/// chosen item arrives separately, as a `WM_COMMAND` the menu's subclass on `owner`
/// turns into a `MenuEvent`.
pub(super) fn show(owner: HWND, menu: HMENU, cursor: POINT) {
    let (at, flags) = anchor(cursor, monitor::work_area(monitor::at(cursor)));
    unsafe {
        // A menu whose owner is not the foreground window stays open when the user
        // clicks somewhere else.
        let _ = SetForegroundWindow(owner);
        let _ = TrackPopupMenu(menu, flags, at.x, at.y, None, owner, None);
        // The documented companion of the call above for notification-area menus:
        // without a message to process, the switch away from `owner` never settles.
        let _ = PostMessageW(Some(owner), WM_NULL, WPARAM(0), LPARAM(0));
    }
}

/// The point to open the menu at, and which way it grows from there.
///
/// `cursor` pulled onto the nearest edge of `work`, with the menu growing away from
/// the taskbar that edge borders — up from one along the bottom, down from one along
/// the top, left from one on the right. A cursor already inside `work` (an icon in
/// the overflow panel, or an auto-hidden taskbar) is left where it is.
fn anchor(cursor: POINT, work: RECT) -> (POINT, TRACK_POPUP_MENU_FLAGS) {
    // An empty work area is Windows failing to report one. Open at the cursor then:
    // over the taskbar, but on the right screen.
    if work.right <= work.left || work.bottom <= work.top {
        return (cursor, TPM_LEFTALIGN | TPM_BOTTOMALIGN);
    }
    let at = POINT {
        x: cursor.x.clamp(work.left, work.right),
        y: cursor.y.clamp(work.top, work.bottom),
    };
    let horizontal = if cursor.x >= work.right {
        TPM_RIGHTALIGN
    } else {
        TPM_LEFTALIGN
    };
    let vertical = if cursor.y < work.top {
        TPM_TOPALIGN
    } else {
        TPM_BOTTOMALIGN
    };
    (at, horizontal | vertical)
}

#[cfg(test)]
mod tests {
    use super::*;

    /// What the taskbar leaves of a screen — 2560x1440 at the origin unless the
    /// test says otherwise.
    fn work(left: i32, top: i32, right: i32, bottom: i32) -> RECT {
        RECT {
            left,
            top,
            right,
            bottom,
        }
    }

    fn at(x: i32, y: i32) -> POINT {
        POINT { x, y }
    }

    #[test]
    fn it_opens_upwards_from_the_top_edge_of_a_bottom_taskbar() {
        // The reported bug: the menu used to open at y = 1420, 20px into a 40px
        // taskbar, and those 20px were what the taskbar hid.
        let (point, flags) = anchor(at(2400, 1420), work(0, 0, 2560, 1400));
        assert_eq!(point, at(2400, 1400));
        assert_eq!(flags, TPM_LEFTALIGN | TPM_BOTTOMALIGN);
    }

    #[test]
    fn it_opens_downwards_from_a_top_taskbar() {
        let (point, flags) = anchor(at(2400, 20), work(0, 40, 2560, 1440));
        assert_eq!(point, at(2400, 40));
        assert_eq!(flags, TPM_LEFTALIGN | TPM_TOPALIGN);
    }

    #[test]
    fn it_opens_to_the_right_of_a_left_taskbar() {
        let (point, flags) = anchor(at(20, 1300), work(60, 0, 2560, 1440));
        assert_eq!(point, at(60, 1300));
        assert_eq!(flags, TPM_LEFTALIGN | TPM_BOTTOMALIGN);
    }

    #[test]
    fn it_opens_to_the_left_of_a_right_taskbar() {
        let (point, flags) = anchor(at(2540, 1300), work(0, 0, 2500, 1440));
        assert_eq!(point, at(2500, 1300));
        assert_eq!(flags, TPM_RIGHTALIGN | TPM_BOTTOMALIGN);
    }

    #[test]
    fn a_cursor_off_the_taskbar_keeps_its_place() {
        // An icon in the overflow panel, which floats above the taskbar, or any
        // icon when the taskbar auto-hides and the work area is the whole screen.
        let (point, flags) = anchor(at(2300, 1340), work(0, 0, 2560, 1400));
        assert_eq!(point, at(2300, 1340));
        assert_eq!(flags, TPM_LEFTALIGN | TPM_BOTTOMALIGN);
    }

    #[test]
    fn it_stays_on_a_monitor_left_of_the_primary() {
        // Negative coordinates: clamping to a fixed origin would throw the menu
        // onto the primary screen.
        let (point, flags) = anchor(at(-200, 1060), work(-1920, 0, 0, 1040));
        assert_eq!(point, at(-200, 1040));
        assert_eq!(flags, TPM_LEFTALIGN | TPM_BOTTOMALIGN);
    }

    #[test]
    fn an_empty_work_area_leaves_the_cursor_alone() {
        let (point, flags) = anchor(at(2400, 1420), RECT::default());
        assert_eq!(point, at(2400, 1420));
        assert_eq!(flags, TPM_LEFTALIGN | TPM_BOTTOMALIGN);
    }
}
