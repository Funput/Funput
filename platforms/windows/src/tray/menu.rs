//! Native tray context-menu construction (right-click IA).

use funput_core::InputMethod;
use tray_icon::menu::{CheckMenuItem, Menu, MenuItem, PredefinedMenuItem};

use super::method_menu::MethodMenu;

pub(super) const ENABLED_ID: &str = "enabled";
pub(super) const SETTINGS_ID: &str = "settings";
pub(super) const UPDATE_ID: &str = "check-update";
pub(super) const QUIT_ID: &str = "quit";

/// Owned menu pieces that must stay alive with the tray icon.
pub(super) struct TrayMenu {
    pub menu: Menu,
    pub methods: MethodMenu,
    pub enabled: CheckMenuItem,
}

/// Build the Phase C menu:
/// Tiếng Việt → methods → Cài đặt / Cập nhật → Thoát (no daily Guide item).
pub(super) fn build(method: InputMethod, on: bool) -> TrayMenu {
    let enabled = CheckMenuItem::with_id(ENABLED_ID, "Tiếng Việt", true, on, None);
    let methods = MethodMenu::new(method);
    let settings = MenuItem::with_id(SETTINGS_ID, "Cài đặt…", true, None);
    let update = MenuItem::with_id(UPDATE_ID, "Kiểm tra cập nhật…", true, None);
    let quit = MenuItem::with_id(QUIT_ID, "Thoát", true, None);

    let menu = Menu::new();
    menu.append(&enabled).expect("append enabled");
    menu.append(&PredefinedMenuItem::separator())
        .expect("append separator");
    methods.append_to(&menu);
    menu.append_items(&[
        &PredefinedMenuItem::separator(),
        &settings,
        &update,
        &PredefinedMenuItem::separator(),
        &quit,
    ])
    .expect("build tray menu");

    TrayMenu {
        menu,
        methods,
        enabled,
    }
}
