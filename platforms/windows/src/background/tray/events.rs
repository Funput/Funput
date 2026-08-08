//! Tray click and context-menu event handling.

use tray_icon::menu::MenuEvent;
use tray_icon::{MouseButton, MouseButtonState, TrayIconEvent};

use super::menu::{QUIT_ID, SETTINGS_ID, UPDATE_ID};
use crate::background::hook;
use crate::ui;

/// Drain pending tray + menu events. Call after each `DispatchMessageW`.
///
/// Child reaping is deliberately not done here: the pump waits on the child's
/// handle directly, so polling it once per dispatched message would only add a
/// `try_wait` syscall to every mouse move the low-level hook delivers.
pub(super) fn drain() {
    while let Ok(ev) = TrayIconEvent::receiver().try_recv() {
        if let TrayIconEvent::Click {
            button: MouseButton::Left,
            button_state: MouseButtonState::Up,
            rect,
            ..
        } = ev
        {
            on_left_click(rect);
        }
    }

    while let Ok(ev) = MenuEvent::receiver().try_recv() {
        handle_menu(ev.id.0.as_str());
    }
}

/// Phase B: open/close the Acrylic Control Center. Toggle VI lives on the flyout
/// (hotkey still refreshes the tray glyph via `ON_TOGGLE`).
fn on_left_click(rect: tray_icon::Rect) {
    ui::toggle_control_center(rect);
}

fn handle_menu(id: &str) {
    match id {
        SETTINGS_ID => ui::launch_settings(false),
        UPDATE_ID => ui::launch_settings(true),
        QUIT_ID => {
            ui::terminate_children();
            hook::quit();
        }
        _ => {}
    }
}
