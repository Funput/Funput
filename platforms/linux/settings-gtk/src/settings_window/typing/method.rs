//! Method and tone-mark placement.

use adw::prelude::*;
use adw::{ComboRow, PreferencesGroup};
use gtk::StringList;

use crate::settings::{Method, Settings, ToneStyle};

pub(super) fn group(settings: &Settings) -> PreferencesGroup {
    let group = PreferencesGroup::new();

    let method_row = ComboRow::builder()
        .title("Phương thức")
        .model(&StringList::new(&Method::ALL.map(Method::label)))
        .build();
    method_row.set_selected(settings.method.index());
    method_row.set_subtitle(settings.method.description());
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
    tone_row.set_selected(match settings.tone_style {
        ToneStyle::Traditional => 0,
        ToneStyle::Modern => 1,
    });
    tone_row.set_subtitle(tone_blurb(settings.tone_style));
    tone_row.connect_selected_notify(|row| {
        let tone = if row.selected() == 0 {
            ToneStyle::Traditional
        } else {
            ToneStyle::Modern
        };
        row.set_subtitle(tone_blurb(tone));
        Settings::update(|settings| settings.tone_style = tone);
    });
    group.add(&tone_row);
    group
}

fn tone_blurb(tone: ToneStyle) -> &'static str {
    match tone {
        ToneStyle::Traditional => "Dấu kiểu cũ — hòa, khỏe, thúy",
        ToneStyle::Modern => "Dấu kiểu mới — hoà, khoẻ, thuý",
    }
}
