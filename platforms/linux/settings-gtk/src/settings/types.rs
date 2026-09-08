use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Method {
    #[serde(rename = "telex")]
    Telex,
    #[serde(rename = "vni")]
    Vni,
    #[serde(rename = "telex_advanced")]
    TelexAdvanced,
}

impl Method {
    pub const ALL: [Self; 3] = [Self::Telex, Self::TelexAdvanced, Self::Vni];

    pub const fn label(self) -> &'static str {
        match self {
            Self::Telex => "Telex",
            Self::TelexAdvanced => "Telex nâng cao",
            Self::Vni => "VNI",
        }
    }

    pub const fn description(self) -> &'static str {
        match self {
            Self::Telex => "Dấu bằng chữ cái — aa→â, ow→ơ, as→á, dd→đ",
            Self::TelexAdvanced => "Full Telex — [→ư, ]→ơ, w đầu từ→ư",
            Self::Vni => "Dấu bằng chữ số — a6→â, o7→ơ, a1→á, d9→đ",
        }
    }

    pub const fn config_key(self) -> &'static str {
        match self {
            Self::Telex => "telex",
            Self::TelexAdvanced => "telex_advanced",
            Self::Vni => "vni",
        }
    }

    pub fn from_config_key(value: &str) -> Option<Self> {
        match value {
            "telex" => Some(Self::Telex),
            "telex_advanced" => Some(Self::TelexAdvanced),
            "vni" => Some(Self::Vni),
            _ => None,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum ToneStyle {
    Traditional,
    #[default]
    Modern,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum Hotkey {
    CtrlBacktick,
    CtrlSpace,
    AltShift,
    SuperSpace,
    CtrlShiftSpace,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum FlipHotkey {
    #[default]
    Off,
    CtrlShiftZ,
    CtrlShiftX,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Shortcut {
    pub trigger: String,
    pub expansion: String,
}
