//! The mouse hook. It exists for one reason: a click moves the text caret, and the
//! keyboard hook cannot see that happen.

use windows::Win32::Foundation::{LPARAM, LRESULT, WPARAM};
use windows::Win32::UI::WindowsAndMessaging::{
    CallNextHookEx, HC_ACTION, WM_LBUTTONDOWN, WM_MBUTTONDOWN, WM_MOUSEMOVE, WM_RBUTTONDOWN,
};

use crate::background::hotkey;
use crate::shared::shell;

/// A button-down click moves the caret, so flush the in-progress composition before
/// the next keystroke diffs against a now-stale word.
pub(super) unsafe extern "system" fn mouse_proc(
    code: i32,
    wparam: WPARAM,
    lparam: LPARAM,
) -> LRESULT {
    if code == HC_ACTION as i32 {
        let msg = wparam.0 as u32;
        if is_caret_moving_click(msg) {
            shell::clear();
        }
        // Any deliberate mouse action ends a modifier-only gesture: Ctrl+Shift
        // then a click is multi-select, not a hotkey. Movement is excluded — it
        // fires on the slightest jitter and would cancel every gesture.
        if msg != WM_MOUSEMOVE {
            hotkey::note_other_input();
        }
    }
    CallNextHookEx(None, code, wparam, lparam)
}

/// Mouse messages that reposition the caret and so must flush composition. Move and
/// wheel events are excluded — they don't move the text caret, and `WM_MOUSEMOVE`
/// fires far too often to take the engine lock on.
fn is_caret_moving_click(msg: u32) -> bool {
    matches!(msg, WM_LBUTTONDOWN | WM_RBUTTONDOWN | WM_MBUTTONDOWN)
}

#[cfg(test)]
mod tests {
    use super::*;
    use windows::Win32::UI::WindowsAndMessaging::WM_MOUSEWHEEL;

    #[test]
    fn button_down_clicks_flush_composition() {
        assert!(is_caret_moving_click(WM_LBUTTONDOWN));
        assert!(is_caret_moving_click(WM_RBUTTONDOWN));
        assert!(is_caret_moving_click(WM_MBUTTONDOWN));
    }

    #[test]
    fn move_and_wheel_do_not_flush() {
        // Excluded so we never take the engine lock on the high-frequency move event.
        assert!(!is_caret_moving_click(WM_MOUSEMOVE));
        assert!(!is_caret_moving_click(WM_MOUSEWHEEL));
    }
}
