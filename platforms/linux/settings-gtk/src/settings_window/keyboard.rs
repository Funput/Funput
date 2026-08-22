//! "Phím tắt" page: the VI/EN toggle hotkey and the flip hotkey.

use adw::prelude::*;
use adw::{ComboRow, PreferencesGroup, PreferencesPage};
use gtk::StringList;

use crate::settings::{FlipHotkey, Hotkey, Settings};

pub(super) fn page() -> PreferencesPage {
    let s = Settings::load();
    let page = PreferencesPage::builder()
        .title("Phím tắt")
        .icon_name("preferences-desktop-keyboard-shortcuts-symbolic")
        .build();
    let group = PreferencesGroup::builder()
        .description("Bật/tắt gõ tiếng Việt nhanh.")
        .build();

    let row = ComboRow::builder()
        .title("Phím tắt")
        .model(&StringList::new(&TOGGLE_LABELS))
        .build();
    row.set_selected(toggle_index(s.toggle_hotkey));
    row.connect_selected_notify(|row| {
        Settings::update(|s| s.toggle_hotkey = toggle_from_index(row.selected()));
    });
    group.add(&row);
    page.add(&group);

    // Flip the word being composed VN↔raw (card ⇄ cải). Presets mirror the Windows
    // build; "Tắt" disables it.
    let flip_group = PreferencesGroup::builder()
        .description("Đổi từ đang gõ giữa tiếng Việt và chữ gốc (card ⇄ cải).")
        .build();
    let flip_row = ComboRow::builder()
        .title("Phím lật từ vừa gõ")
        .model(&StringList::new(&[
            "Tắt",
            "Ctrl + Shift + Z",
            "Ctrl + Shift + X",
        ]))
        .build();
    flip_row.set_selected(match s.flip_hotkey {
        FlipHotkey::CtrlShiftZ => 1,
        FlipHotkey::CtrlShiftX => 2,
        FlipHotkey::Off => 0,
    });
    flip_row.connect_selected_notify(|row| {
        let h = match row.selected() {
            1 => FlipHotkey::CtrlShiftZ,
            2 => FlipHotkey::CtrlShiftX,
            _ => FlipHotkey::Off,
        };
        Settings::update(|s| s.flip_hotkey = h);
    });
    flip_group.add(&flip_row);
    page.add(&flip_group);

    page
}

const TOGGLE_LABELS: [&str; 5] = [
    "Ctrl + `",
    "Ctrl + Space",
    "Alt + Shift",
    "Super + Space",
    "Ctrl + Shift + Space",
];

const TOGGLE_PRESETS: [Hotkey; 5] = [
    Hotkey::CtrlBacktick,
    Hotkey::CtrlSpace,
    Hotkey::AltShift,
    Hotkey::SuperSpace,
    Hotkey::CtrlShiftSpace,
];

fn toggle_index(hotkey: Hotkey) -> u32 {
    TOGGLE_PRESETS
        .iter()
        .position(|preset| *preset == hotkey)
        .unwrap_or(0) as u32
}

fn toggle_from_index(index: u32) -> Hotkey {
    TOGGLE_PRESETS
        .get(index as usize)
        .copied()
        .unwrap_or(Hotkey::CtrlBacktick)
}
