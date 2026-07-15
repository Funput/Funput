//! A user-recorded hotkey combo — the "Tùy chỉnh" alternative to the fixed
//! presets. Serialized into `settings.json` and the shared config document as
//! `{ vk, ctrl, alt, shift, win, label }`.

use serde::{Deserialize, Serialize};

/// One recorded combo: the Win32 virtual-key of the main key plus the exact
/// modifier set. `label` is the main key's display text captured at record
/// time, so rendering never needs a VK → name table for the user's layout.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct KeyCombo {
    pub vk: u16,
    #[serde(default)]
    pub ctrl: bool,
    #[serde(default)]
    pub alt: bool,
    #[serde(default)]
    pub shift: bool,
    #[serde(default)]
    pub win: bool,
    pub label: String,
}

impl KeyCombo {
    /// The keycaps shown in the UI, e.g. `["Ctrl", "Shift", "V"]` — same shape
    /// as the preset `caps()` so the "Đang dùng" row renders either source.
    pub fn caps(&self) -> Vec<String> {
        let mut caps = Vec::new();
        if self.ctrl {
            caps.push("Ctrl".to_string());
        }
        if self.alt {
            caps.push("Alt".to_string());
        }
        if self.shift {
            caps.push("Shift".to_string());
        }
        if self.win {
            caps.push("Win".to_string());
        }
        caps.push(self.label.clone());
        caps
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn serializes_camel_case_and_round_trips() {
        let combo = KeyCombo {
            vk: 0x56,
            ctrl: true,
            alt: false,
            shift: true,
            win: false,
            label: "V".into(),
        };
        let json = serde_json::to_string(&combo).unwrap();
        assert!(json.contains("\"vk\":86"), "{json}");
        assert!(json.contains("\"ctrl\":true"), "{json}");
        assert_eq!(serde_json::from_str::<KeyCombo>(&json).unwrap(), combo);
    }

    #[test]
    fn caps_lists_modifiers_then_label() {
        let combo = KeyCombo {
            vk: 0x20,
            ctrl: true,
            alt: true,
            shift: false,
            win: false,
            label: "Space".into(),
        };
        assert_eq!(combo.caps(), ["Ctrl", "Alt", "Space"]);
    }
}
