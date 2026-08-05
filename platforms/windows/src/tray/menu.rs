//! Native tray context-menu construction (right-click IA).

use tray_icon::menu::{Menu, MenuItem, PredefinedMenuItem};

pub(super) const SETTINGS_ID: &str = "settings";
pub(super) const UPDATE_ID: &str = "check-update";
pub(super) const QUIT_ID: &str = "quit";

/// Phase B thin menu: Settings / Update / Quit (VI + methods live in the flyout).
pub(super) fn build() -> Menu {
    let settings = MenuItem::with_id(SETTINGS_ID, "Cài đặt…", true, None);
    let update = MenuItem::with_id(UPDATE_ID, "Kiểm tra cập nhật…", true, None);
    let quit = MenuItem::with_id(QUIT_ID, "Thoát", true, None);
    let menu = Menu::new();
    menu.append_items(&[
        &settings,
        &update,
        &PredefinedMenuItem::separator(),
        &quit,
    ])
    .expect("build tray menu");
    menu
}
