//! How the composing word reaches the client (preedit vs document repair).
//! Featured on Tổng quan — both Linux shells perform an `Effect::Replace`.
//! A client that cannot report surrounding text keeps the preedit, so the
//! switch is never a no-op.

use adw::prelude::*;
use adw::{PreferencesGroup, SwitchRow};

use crate::settings::Settings;

pub(super) fn group(settings: &Settings) -> PreferencesGroup {
    let group = PreferencesGroup::builder()
        .title("Gõ thẳng vào ứng dụng")
        .description(
            "Chữ hiện ngay trong ô gõ, không nằm ở dòng gạch chân. Đổi cửa sổ giữa chừng cũng không mất chữ.",
        )
        .build();
    let row = SwitchRow::builder()
        .title("Bật gõ thẳng")
        .subtitle("Đang thử nghiệm. App nào chưa hỗ trợ thì Funput tự về cách cũ.")
        .active(settings.non_preedit)
        .build();
    row.add_prefix(&gtk::Image::from_icon_name("document-edit-symbolic"));
    row.connect_active_notify(|row| {
        let on = row.is_active();
        Settings::update(|settings| settings.non_preedit = on);
    });
    group.add(&row);
    group
}
