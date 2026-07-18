//! "Kiểu gõ" page: input method and tone-mark placement style.

use adw::prelude::*;
use adw::{ComboRow, PreferencesGroup, PreferencesPage};
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
    page
}
