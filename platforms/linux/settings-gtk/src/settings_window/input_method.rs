//! "Kiểu gõ" page: input method and tone-mark placement style.

use adw::prelude::*;
use adw::{ComboRow, PreferencesGroup, PreferencesPage, SwitchRow};
use gtk::StringList;

use crate::settings::{Method, Settings, ToneStyle};

fn tone_blurb(t: ToneStyle) -> &'static str {
    match t {
        ToneStyle::Traditional => "Dấu kiểu cũ — hòa, khỏe, thúy",
        ToneStyle::Modern => "Dấu kiểu mới — hoà, khoẻ, thuý",
    }
}

pub(super) fn page() -> PreferencesPage {
    let s = Settings::load();
    let page = PreferencesPage::builder()
        .title("Kiểu gõ")
        .icon_name("input-keyboard-symbolic")
        .build();
    let group = PreferencesGroup::new();

    let method_row = ComboRow::builder()
        .title("Phương thức")
        .model(&StringList::new(&Method::ALL.map(Method::label)))
        .build();
    method_row.set_selected(s.method.index());
    method_row.set_subtitle(s.method.description());
    method_row.connect_selected_notify(|row| {
        let method = Method::from_index(row.selected());
        row.set_subtitle(method.description());
        Settings::update(|settings| settings.method = method);
    });
    group.add(&method_row);

    let tone_row = ComboRow::builder()
        .title("Kiểu đặt dấu")
        .model(&StringList::new(&["Truyền thống", "Hiện đại"]))
        .build();
    tone_row.set_selected(match s.tone_style {
        ToneStyle::Traditional => 0,
        ToneStyle::Modern => 1,
    });
    tone_row.set_subtitle(tone_blurb(s.tone_style));
    tone_row.connect_selected_notify(|row| {
        let t = if row.selected() == 0 {
            ToneStyle::Traditional
        } else {
            ToneStyle::Modern
        };
        row.set_subtitle(tone_blurb(t));
        Settings::update(|s| s.tone_style = t);
    });
    group.add(&tone_row);

    page.add(&group);
    // How the word reaches the app, which is a different question from which keys
    // produce it — hence its own group rather than a third row above.
    page.add(&delivery_group(&s));
    page
}

/// The non-preedit switch. Unlike the per-app page, this is shown on every framework:
/// both Linux shells perform an `Effect::Replace`, and a client that cannot report
/// surrounding text simply keeps the preedit, so the switch is never a control that
/// does nothing.
fn delivery_group(s: &Settings) -> PreferencesGroup {
    let group = PreferencesGroup::builder()
        .title("Cách chữ vào ứng dụng")
        .build();
    let row = SwitchRow::builder()
        .title("Gõ thẳng vào ứng dụng (thử nghiệm)")
        .subtitle(
            "Chữ đi thẳng vào tài liệu thay vì nằm ở dòng gạch chân, nên không mất khi \
             đổi cửa sổ giữa chừng. Ứng dụng nào không hỗ trợ thì tự quay về cách cũ.",
        )
        .active(s.non_preedit)
        .build();
    row.connect_active_notify(|row| {
        let on = row.is_active();
        Settings::update(|s| s.non_preedit = on);
    });
    group.add(&row);
    group
}
