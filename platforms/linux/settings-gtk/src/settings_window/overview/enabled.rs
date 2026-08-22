//! VI/EN switch — same `Settings.enabled` key as the toggle hotkey.

use adw::prelude::*;
use adw::{PreferencesGroup, SwitchRow};

use crate::settings::Settings;

pub(super) fn group(settings: &Settings) -> PreferencesGroup {
    let group = PreferencesGroup::builder().title("Bộ gõ").build();
    let row = SwitchRow::builder()
        .title("Gõ tiếng Việt")
        .subtitle("Tắt để gõ tiếng Anh.")
        .active(settings.enabled)
        .build();
    row.add_prefix(&gtk::Image::from_icon_name(
        "preferences-desktop-locale-symbolic",
    ));
    row.connect_active_notify(|row| {
        let on = row.is_active();
        Settings::update(|settings| settings.enabled = on);
    });
    group.add(&row);
    group
}
