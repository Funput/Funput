//! The Control Center flyout's lifecycle.
//!
//! It differs from Settings and Onboarding in two ways that need their own logic:
//! it toggles from the same tray click that dismisses it, and it reports what the
//! user picked through its **exit code** rather than any shared state.

use std::sync::atomic::{AtomicU64, Ordering};

use tray_icon::Rect;

use super::child::{
    launch_settings_tab, now_ms, spawn_child, terminate_children, UiKind, UI_PROCESS,
};
use super::control_center;
use crate::background::tray;
use crate::shared::shell;

/// Last Control Center dismiss (ms); debounce tray re-open after focus-loss.
static LAST_CC_DISMISS_MS: AtomicU64 = AtomicU64::new(0);

const TRAY_X: &str = "FUNPUT_TRAY_X";
const TRAY_Y: &str = "FUNPUT_TRAY_Y";
const TRAY_W: &str = "FUNPUT_TRAY_W";
const TRAY_H: &str = "FUNPUT_TRAY_H";

pub(crate) fn toggle_control_center(rect: Rect) {
    reap_ui_child();
    let is_cc = UI_PROCESS.with(|cell| {
        cell.borrow()
            .as_ref()
            .is_some_and(|(kind, _)| *kind == UiKind::ControlCenter)
    });
    if is_cc {
        terminate_children();
        if shell::reload_settings() {
            tray::sync_from_shell();
        }
        return;
    }
    // Tray click unfocuses the flyout first; don't reopen in the same gesture.
    if now_ms().saturating_sub(LAST_CC_DISMISS_MS.load(Ordering::SeqCst)) < 400 {
        return;
    }
    terminate_children();
    let x = rect.position.x.to_string();
    let y = rect.position.y.to_string();
    let w = rect.size.width.to_string();
    let h = rect.size.height.to_string();
    spawn_child(
        "--control-center",
        UiKind::ControlCenter,
        &[(TRAY_X, &x), (TRAY_Y, &y), (TRAY_W, &w), (TRAY_H, &h)],
    );
}

pub(crate) fn reap_ui_child() {
    let finished = UI_PROCESS.with(|cell| {
        let mut slot = cell.borrow_mut();
        let (kind, child) = slot.as_mut()?;
        let out = (*kind, child.exit_code()? as u8);
        *slot = None; // dropping it here is what closes the process handle
        Some(out)
    });
    let Some((UiKind::ControlCenter, code)) = finished else {
        return;
    };
    if shell::reload_settings() {
        tray::sync_from_shell();
    }
    if code == control_center::EXIT_DISMISS {
        LAST_CC_DISMISS_MS.store(now_ms(), Ordering::SeqCst);
        return;
    }
    match code {
        control_center::EXIT_SETTINGS => launch_settings_tab("overview", false),
        control_center::EXIT_KEYBOARD => launch_settings_tab("keyboard", false),
        _ => {}
    }
}
