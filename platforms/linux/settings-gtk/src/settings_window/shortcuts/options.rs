//! The two switches governing the gõ tắt table, above the rows themselves.
//!
//! Kept visible whether or not the table has any rows — an empty table is exactly
//! when a user is most likely to be reading this page — and mirroring the Windows
//! page (`platforms/windows/ui/pages/shortcuts/page.slint`) row for row.

use adw::prelude::*;
use adw::{PreferencesGroup, SwitchRow};

use crate::settings::Settings;

/// This group's description, which has to follow the smart-case switch: the example
/// stops being true the moment case matching is off. It sits here rather than on the
/// list below because the list is hidden while the table is empty, and a switch
/// whose explanation vanishes with the rows explains nothing.
fn description(smart_case: bool) -> &'static str {
    if smart_case {
        "Gõ chữ tắt rồi dấu cách để bung — ví dụ vn → việt nam. Tự nhận diện hoa/thường: \
         Vn → Việt Nam, VN → VIỆT NAM."
    } else {
        "Gõ chữ tắt rồi dấu cách để bung — ví dụ vn → việt nam. Đang tắt nhận diện \
         hoa/thường: chỉ đúng chữ tắt đã lưu mới bung, nội dung giữ nguyên."
    }
}

pub(super) fn group(settings: &Settings) -> PreferencesGroup {
    let group = PreferencesGroup::builder()
        .title("Gõ tắt")
        .description(description(settings.shortcut_smart_case))
        .build();

    let enabled_row = SwitchRow::builder()
        .title("Bật gõ tắt")
        .subtitle("Tắt để tạm gõ nguyên chữ tắt — bảng bên dưới vẫn giữ nguyên.")
        .active(settings.shortcuts_enabled)
        .build();
    enabled_row.connect_active_notify(|row| {
        let on = row.is_active();
        Settings::update(|settings| settings.shortcuts_enabled = on);
    });

    let smart_row = SwitchRow::builder()
        .title("Tự nhận diện hoa/thường")
        .subtitle(
            "Gõ vn, Vn hay VN đều bung và nội dung tự viết hoa theo. Tắt thì chỉ khớp \
             đúng chuỗi tắt đã lưu.",
        )
        .active(settings.shortcut_smart_case)
        .build();
    // Deliberately not desensitized when "Bật gõ tắt" is off, even though it depends
    // on it: macOS leaves the switch live, and the platforms must read the same.
    let group_for_smart = group.clone();
    smart_row.connect_active_notify(move |row| {
        let on = row.is_active();
        Settings::update(|settings| settings.shortcut_smart_case = on);
        group_for_smart.set_description(Some(description(on)));
    });

    group.add(&enabled_row);
    group.add(&smart_row);
    group
}
