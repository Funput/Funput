//! How the composing word reaches the client (preedit vs document repair).
//! Featured on Tổng quan — both Linux shells perform an `Effect::Replace`.
//! A client that cannot report surrounding text keeps the preedit, so the
//! switch is never a no-op.

use adw::prelude::*;
use adw::{PreferencesGroup, SwitchRow};

use crate::settings::Settings;

pub(super) fn group(settings: &Settings) -> PreferencesGroup {
    let group = PreferencesGroup::builder()
        .title("Cách chữ hiện ra khi gõ")
        .description(
            "Có hai kiểu: chữ vào thẳng ô nhập, hoặc đợi ở một dòng gạch chân rồi mới đáp xuống — đổi cửa sổ giữa chừng thì kiểu sau làm mất từ đang gõ.",
        )
        .build();
    // Named for what the user sees, not for the mechanism: "preedit" is a word this
    // audience has no reason to know, while the underline is on their screen. Someone
    // arriving from UniKey on Windows has only ever seen the direct kind, so the
    // subtitle says so rather than explaining a concept they never had.
    let row = SwitchRow::builder()
        .title("Gõ thẳng, không gạch chân")
        .subtitle("Giống UniKey trên Windows. App nào không nhận được thì Funput tự về cách cũ.")
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
