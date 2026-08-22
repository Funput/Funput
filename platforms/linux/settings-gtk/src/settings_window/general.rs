//! "Chung" page: session preference, config backup, and About.

use adw::prelude::*;
use adw::{PreferencesGroup, PreferencesPage, PreferencesWindow, SwitchRow};

use crate::settings::Settings;

mod about;
mod transfer;

pub(super) fn page(window: &PreferencesWindow) -> PreferencesPage {
    let settings = Settings::load();
    let page = PreferencesPage::builder()
        .title("Chung")
        .icon_name("preferences-system-symbolic")
        .build();

    let group = PreferencesGroup::new();
    // On Linux the engine runs inside the fcitx5/ibus daemon, whose autostart is
    // managed by the desktop session — this toggle only persists the preference.
    let row = SwitchRow::builder()
        .title("Khởi động cùng phiên đăng nhập")
        .subtitle("Bộ gõ do desktop quản lý tự khởi động; tuỳ chọn này chỉ được lưu lại.")
        .active(settings.launch_at_login)
        .build();
    row.connect_active_notify(|row| {
        let on = row.is_active();
        Settings::update(|settings| settings.launch_at_login = on);
    });
    group.add(&row);
    page.add(&group);

    page.add(&transfer::group(window));

    let about_group = PreferencesGroup::new();
    about_group.add(&about::row(window));
    page.add(&about_group);
    page
}
