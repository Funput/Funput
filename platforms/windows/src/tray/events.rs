//! Drain tray icon clicks and context-menu actions.

use tray_icon::menu::MenuEvent;
use tray_icon::{MouseButton, MouseButtonState, TrayIconEvent};

use super::menu::{ENABLED_ID, QUIT_ID, SETTINGS_ID, UPDATE_ID};
use super::method_menu;
use super::{refresh, sync_method};
use crate::{hook, shell, windows_ui};

/// Drain pending tray + menu events. Call after each `DispatchMessageW`.
pub(super) fn drain() {
    while let Ok(ev) = TrayIconEvent::receiver().try_recv() {
        if let TrayIconEvent::Click {
            button: MouseButton::Left,
            button_state: MouseButtonState::Up,
            ..
        } = ev
        {
            on_left_click();
        }
    }

    while let Ok(ev) = MenuEvent::receiver().try_recv() {
        handle_menu(ev.id.0.as_str());
    }
}

/// Phase C: toggle VI/EN. Phase B will open the Acrylic popover here instead;
/// toggle then lives on the popover (hotkey still refreshes the tray).
fn on_left_click() {
    let on = shell::toggle_enabled();
    refresh(on);
}

fn handle_menu(id: &str) {
    if let Some(method) = method_menu::method_for_id(id) {
        shell::set_method(method);
        sync_method(method);
        // Method change while VI is on should refresh the tooltip label.
        refresh(shell::enabled());
        return;
    }
    match id {
        ENABLED_ID => {
            let on = shell::toggle_enabled();
            refresh(on);
        }
        SETTINGS_ID => windows_ui::launch_settings(false),
        UPDATE_ID => windows_ui::launch_settings(true),
        QUIT_ID => {
            windows_ui::terminate_children();
            hook::quit();
        }
        _ => {}
    }
}
