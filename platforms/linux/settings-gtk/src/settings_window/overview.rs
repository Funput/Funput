//! "Tổng quan" — session preference for now. Step 3 can add status metrics here.

use adw::prelude::*;
use adw::{PreferencesGroup, PreferencesPage, SwitchRow};

use crate::settings::Settings;

pub(super) fn page() -> PreferencesPage {
    let settings = Settings::load();
    let page = PreferencesPage::builder()
        .title("Tổng quan")
        .icon_name("preferences-system-symbolic")
        .build();

    let group = PreferencesGroup::new();
    // The engine lives in the fcitx5/ibus daemon; the desktop starts that. This
    // toggle only persists the preference until a later step can honor it.
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
    page
}
