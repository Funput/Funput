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

    pub fn index(self) -> u32 {
        Self::ALL
            .iter()
            .position(|method| *method == self)
            .unwrap_or(0) as u32
    }

    pub fn from_index(index: u32) -> Self {
        Self::ALL
            .get(index as usize)
            .copied()
            .unwrap_or(Self::Telex)
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum ToneStyle {
    #[default]
    Traditional,
    Modern,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum Hotkey {
    CtrlBacktick,
    CtrlSpace,
    AltShift,
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

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Settings {
    pub method: Method,
    #[serde(default)]
    pub tone_style: ToneStyle,
    pub enabled: bool,
    pub smart_restore: bool,
    pub eager_restore: bool,
    #[serde(default)]
    pub spell_check: bool,
    #[serde(default)]
    pub auto_capitalize: bool,
    /// Read and written but not shown: no shell performs non-preedit yet. Carried
    /// so `save()` does not drop it — see the note there.
    #[serde(default)]
    pub non_preedit: bool,
    pub toggle_hotkey: Hotkey,
    #[serde(default)]
    pub flip_hotkey: FlipHotkey,
    pub launch_at_login: bool,
    pub has_completed_onboarding: bool,
    #[serde(default)]
    pub shortcuts: Vec<Shortcut>,
}

impl Default for Settings {
    fn default() -> Self {
        Self {
            method: Method::Vni,
            tone_style: ToneStyle::Traditional,
            enabled: true,
            smart_restore: true,
            eager_restore: true,
            spell_check: false,
            auto_capitalize: false,
            non_preedit: false,
            toggle_hotkey: Hotkey::CtrlBacktick,
            flip_hotkey: FlipHotkey::Off,
            launch_at_login: false,
            has_completed_onboarding: false,
            shortcuts: Vec::new(),
        }
    }
}
