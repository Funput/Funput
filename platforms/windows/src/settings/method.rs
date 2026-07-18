use funput_core::{InputMethod, ToneStyle as CoreToneStyle};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum Method {
    Telex,
    Vni,
}

impl Method {
    pub fn core(self) -> InputMethod {
        match self {
            Self::Telex => InputMethod::Telex,
            Self::Vni => InputMethod::Vni,
        }
    }

    /// Compile-only compatibility until Windows exposes Telex Advanced.
    pub fn from_core(method: InputMethod) -> Self {
        match method {
            InputMethod::Telex | InputMethod::TelexAdvanced => Self::Telex,
            InputMethod::Vni => Self::Vni,
            _ => unreachable!("Windows must integrate a newly added input method"),
        }
    }

    pub fn id(self) -> &'static str {
        match self {
            Self::Telex => "telex",
            Self::Vni => "vni",
        }
    }

    pub fn from_id(id: &str) -> Option<Self> {
        match id {
            "telex" => Some(Self::Telex),
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

impl ToneStyle {
    pub fn core(self) -> CoreToneStyle {
        match self {
            Self::Traditional => CoreToneStyle::Traditional,
            Self::Modern => CoreToneStyle::Modern,
        }
    }

    pub fn from_core(style: CoreToneStyle) -> Self {
        match style {
            CoreToneStyle::Traditional => Self::Traditional,
            CoreToneStyle::Modern => Self::Modern,
        }
    }

    pub fn id(self) -> &'static str {
        match self {
            Self::Traditional => "traditional",
            Self::Modern => "modern",
        }
    }

    pub fn from_id(id: &str) -> Option<Self> {
        match id {
            "traditional" => Some(Self::Traditional),
            "modern" => Some(Self::Modern),
            _ => None,
        }
    }
}
