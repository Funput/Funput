//! English auto-restore and related typing helpers.

use adw::prelude::*;
use adw::{PreferencesGroup, SwitchRow};

use crate::settings::Settings;

pub(super) fn group(settings: &Settings) -> PreferencesGroup {
    let group = PreferencesGroup::builder().title("Gõ thông minh").build();

    let smart_row = SwitchRow::builder()
        .title("Tự khôi phục tiếng Anh")
        .subtitle("Từ không phải tiếng Việt giữ nguyên chữ gốc (card → card, không thành cảd).")
        .active(settings.smart_restore)
        .build();
    let eager_row = SwitchRow::builder()
        .title("Khôi phục tức thì")
        .subtitle("Đổi lại ngay khi từ không thể là tiếng Việt, không chờ dấu cách.")
        .active(settings.eager_restore)
        .build();
    eager_row.set_sensitive(settings.smart_restore);

    let eager_for_smart = eager_row.clone();
    smart_row.connect_active_notify(move |row| {
        let on = row.is_active();
        eager_for_smart.set_sensitive(on);
        Settings::update(|settings| settings.smart_restore = on);
    });
    eager_row.connect_active_notify(|row| {
        let on = row.is_active();
        Settings::update(|settings| settings.eager_restore = on);
    });

    let spell_row = SwitchRow::builder()
        .title("Kiểm tra chính tả")
        .subtitle("Chỉ đặt dấu khi tạo thành âm tiết tiếng Việt hợp lệ.")
        .active(settings.spell_check)
        .build();
    spell_row.connect_active_notify(|row| {
        let on = row.is_active();
        Settings::update(|settings| settings.spell_check = on);
    });

    let auto_cap_row = SwitchRow::builder()
        .title("Tự động viết hoa")
        .subtitle("Viết hoa chữ đầu câu, sau dấu chấm và đầu dòng.")
        .active(settings.auto_capitalize)
        .build();
    auto_cap_row.connect_active_notify(|row| {
        let on = row.is_active();
        Settings::update(|settings| settings.auto_capitalize = on);
    });

    group.add(&smart_row);
    group.add(&eager_row);
    group.add(&spell_row);
    group.add(&auto_cap_row);
    group
}
