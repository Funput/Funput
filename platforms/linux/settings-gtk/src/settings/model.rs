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
    /// Hidden from Settings UI; kept so wholesale `save()` does not drop the key.
    pub launch_at_login: bool,
    pub has_completed_onboarding: bool,
    #[serde(default)]
    pub shortcuts: Vec<Shortcut>,
    /// Whether the table above expands at all. The rows stay stored either way.
    #[serde(default = "on")]
    pub shortcuts_enabled: bool,
    /// Whether a trigger matches however it was capitalized, with the expansion
    /// re-cased to match.
    ///
    /// Both default to **on**, including for a file written before they existed:
    /// that is how gõ tắt has always behaved, and an update must not silently change
    /// what an existing table expands to. The addon's C++ model says the same.
    #[serde(default = "on")]
    pub shortcut_smart_case: bool,
}

fn on() -> bool {
    true
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
            shortcuts_enabled: true,
            shortcut_smart_case: true,
        }
    }
}
