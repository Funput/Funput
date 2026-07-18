use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum Hotkey {
    CtrlBacktick,
    CtrlSpace,
    AltShift,
}

impl Hotkey {
    pub fn id(self) -> &'static str {
        match self {
            Self::CtrlBacktick => "ctrl_backtick",
            Self::CtrlSpace => "ctrl_space",
            Self::AltShift => "alt_shift",
        }
    }

    pub fn from_id(id: &str) -> Option<Self> {
        match id {
            "ctrl_backtick" => Some(Self::CtrlBacktick),
            "ctrl_space" => Some(Self::CtrlSpace),
            "alt_shift" => Some(Self::AltShift),
            _ => None,
        }
    }

    pub fn caps(self) -> &'static [&'static str] {
        match self {
            Self::CtrlBacktick => &["Ctrl", "`"],
            Self::CtrlSpace => &["Ctrl", "Space"],
            Self::AltShift => &["Alt", "Shift"],
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum FlipHotkey {
    #[default]
    Off,
    CtrlShiftZ,
    CtrlShiftX,
}

impl FlipHotkey {
    pub fn id(self) -> &'static str {
        match self {
            Self::Off => "off",
            Self::CtrlShiftZ => "ctrl_shift_z",
            Self::CtrlShiftX => "ctrl_shift_x",
        }
    }

    pub fn from_id(id: &str) -> Option<Self> {
        match id {
            "off" => Some(Self::Off),
            "ctrl_shift_z" => Some(Self::CtrlShiftZ),
            "ctrl_shift_x" => Some(Self::CtrlShiftX),
            _ => None,
        }
    }

    pub fn caps(self) -> &'static [&'static str] {
        match self {
            Self::Off => &["—"],
            Self::CtrlShiftZ => &["Ctrl", "Shift", "Z"],
            Self::CtrlShiftX => &["Ctrl", "Shift", "X"],
        }
    }
}
