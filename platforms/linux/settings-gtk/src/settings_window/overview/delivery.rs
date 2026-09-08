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
    // audience has no reason to know, while the underline is on their screen. The
    // subtitle spends itself on the one thing the switch cannot promise — the mode
    // stands itself down for a client that cannot take a document repair — and names
    // the fallback by the same underline the title does, so "the old way" is not left
    // to the reader. No other product is named here: Funput's own Windows shell types
    // straight into the document too, so borrowing someone else's name for our own
    // behaviour would be both unnecessary and wrong.
    let row = SwitchRow::builder()
        .title("Gõ thẳng, không gạch chân")
        .subtitle("App nào không nhận được thì Funput tự chuyển về kiểu gạch chân.")
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
