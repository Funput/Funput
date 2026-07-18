use crate::settings::{FlipHotkey, Hotkey, ToneStyle};

pub(super) const fn tone_key(tone: ToneStyle) -> &'static str {
    match tone {
        ToneStyle::Traditional => "traditional",
        ToneStyle::Modern => "modern",
    }
}

pub(super) fn tone_from_key(value: &str) -> Option<ToneStyle> {
    match value {
        "traditional" => Some(ToneStyle::Traditional),
        "modern" => Some(ToneStyle::Modern),
        _ => None,
    }
}

pub(super) const fn hotkey_key(hotkey: Hotkey) -> &'static str {
    match hotkey {
        Hotkey::CtrlBacktick => "ctrl_backtick",
        Hotkey::CtrlSpace => "ctrl_space",
        Hotkey::AltShift => "alt_shift",
    }
}

pub(super) fn hotkey_from_key(value: &str) -> Option<Hotkey> {
    match value {
        "ctrl_backtick" => Some(Hotkey::CtrlBacktick),
        "ctrl_space" => Some(Hotkey::CtrlSpace),
        "alt_shift" => Some(Hotkey::AltShift),
        _ => None,
    }
}

pub(super) const fn flip_key(hotkey: FlipHotkey) -> &'static str {
    match hotkey {
        FlipHotkey::Off => "off",
        FlipHotkey::CtrlShiftZ => "ctrl_shift_z",
        FlipHotkey::CtrlShiftX => "ctrl_shift_x",
    }
}

pub(super) fn flip_from_key(value: &str) -> Option<FlipHotkey> {
    match value {
        "off" => Some(FlipHotkey::Off),
        "ctrl_shift_z" => Some(FlipHotkey::CtrlShiftZ),
        "ctrl_shift_x" => Some(FlipHotkey::CtrlShiftX),
        _ => None,
    }
}
