//! Method picker (radio rows) and tone-mark placement.

use adw::prelude::*;
use adw::{ActionRow, ComboRow, PreferencesGroup};
use gtk::{CheckButton, StringList};

use crate::settings::{Method, Settings, ToneStyle};

pub(super) fn group(settings: &Settings) -> PreferencesGroup {
    let group = PreferencesGroup::builder().title("Phương thức").build();
    let mut radio: Option<CheckButton> = None;
    for method in Method::ALL {
        let check = CheckButton::new();
        if let Some(leader) = &radio {
            check.set_group(Some(leader));
        } else {
            radio = Some(check.clone());
        }
        check.set_active(method == settings.method);
        check.connect_toggled(move |check| {
            if check.is_active() {
                Settings::update(|settings| settings.method = method);
            }
        });
        let row = ActionRow::builder()
            .title(method.label())
            .subtitle(method.description())
            .activatable(true)
            .build();
        row.add_prefix(&check);
        row.set_activatable_widget(Some(&check));
        group.add(&row);
    }
    group.add(&tone_row(settings));
    group
}

fn tone_row(settings: &Settings) -> ComboRow {
    let row = ComboRow::builder()
        .title("Kiểu đặt dấu")
        .model(&StringList::new(&["Truyền thống", "Hiện đại"]))
        .build();
    row.set_selected(match settings.tone_style {
        ToneStyle::Traditional => 0,
        ToneStyle::Modern => 1,
    });
    row.set_subtitle(tone_blurb(settings.tone_style));
    row.connect_selected_notify(|row| {
        let tone = if row.selected() == 0 {
            ToneStyle::Traditional
        } else {
            ToneStyle::Modern
        };
        row.set_subtitle(tone_blurb(tone));
        Settings::update(|settings| settings.tone_style = tone);
    });
    row
}

fn tone_blurb(tone: ToneStyle) -> &'static str {
    match tone {
        ToneStyle::Traditional => "Dấu kiểu cũ — hòa, khỏe, thúy",
        ToneStyle::Modern => "Dấu kiểu mới — hoà, khoẻ, thuý",
    }
}
