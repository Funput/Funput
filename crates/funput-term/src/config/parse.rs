//! Environment-variable overlay and key/enum spec parsing.
//!
//! Higher precedence than the settings file; the getter is injected so this is
//! unit-testable without touching the process environment.

use funput_core::{InputMethod, ToneStyle};

use super::TermConfig;

impl TermConfig {
    /// Overlay environment-variable overrides (higher precedence than the file).
    /// The getter is injected so this is unit-testable without touching the
    /// process environment.
    pub fn apply_env(mut self, get: impl Fn(&str) -> Option<String>) -> Self {
        if let Some(m) = get("FUNPUT_METHOD").as_deref().and_then(parse_method) {
            self.method = m;
        }
        if let Some(t) = get("FUNPUT_TONE_STYLE").as_deref().and_then(parse_tone) {
            self.tone_style = t;
        }
        if let Some(e) = get("FUNPUT_ENABLED").as_deref().and_then(parse_bool) {
            self.enabled = e;
        }
        if let Some(b) = get("FUNPUT_TOGGLE").as_deref().and_then(parse_toggle) {
            self.toggle = b;
        }
        if let Some(spec) = get("FUNPUT_CYCLE_METHOD") {
            // `off`/`none`/empty disables; otherwise parse a key spec (invalid
            // specs are ignored, keeping the previous value).
            match spec.trim().to_ascii_lowercase().as_str() {
                "" | "off" | "none" | "disable" | "disabled" => self.cycle_method = None,
                _ => {
                    if let Some(b) = parse_toggle(&spec) {
                        self.cycle_method = Some(b);
                    }
                }
            }
        }
        if let Some(c) = get("FUNPUT_CURSOR_COLOR_VI").filter(|c| !c.is_empty()) {
            self.vi_cursor_color = c;
        }
        self
    }
}

fn parse_method(s: &str) -> Option<InputMethod> {
    match s.to_ascii_lowercase().as_str() {
        "telex" => Some(InputMethod::Telex),
        "vni" => Some(InputMethod::Vni),
        _ => None,
    }
}

fn parse_tone(s: &str) -> Option<ToneStyle> {
    match s.to_ascii_lowercase().as_str() {
        "traditional" | "old" => Some(ToneStyle::Traditional),
        "modern" | "new" => Some(ToneStyle::Modern),
        _ => None,
    }
}

fn parse_bool(s: &str) -> Option<bool> {
    match s.to_ascii_lowercase().as_str() {
        "1" | "true" | "yes" | "on" => Some(true),
        "0" | "false" | "no" | "off" => Some(false),
        _ => None,
    }
}

/// Parse a toggle-key spec into its control byte: `ctrl-\`, `ctrl-^`, `ctrl-]`,
/// `ctrl-space`, etc. A control byte is the key's ASCII value masked with 0x1f
/// (`\` 0x5c → 0x1c), and `space` maps to NUL (0x00, i.e. `Ctrl-Space`).
fn parse_toggle(s: &str) -> Option<u8> {
    let key = s.trim().to_ascii_lowercase();
    let key = key
        .strip_prefix("ctrl-")
        .or_else(|| key.strip_prefix("c-"))?;
    match key {
        "space" | "spc" => Some(0x00),
        k if k.chars().count() == 1 => {
            let ch = k.chars().next().unwrap();
            ch.is_ascii().then_some((ch as u8) & 0x1f)
        }
        _ => None,
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::config::{DEFAULT_CYCLE_METHOD, from_json};

    #[test]
    fn env_overrides_file_values() {
        let base = from_json(r#"{ "method": "vni", "enabled": true }"#);
        let env = |k: &str| match k {
            "FUNPUT_METHOD" => Some("telex".to_string()),
            "FUNPUT_ENABLED" => Some("false".to_string()),
            "FUNPUT_TOGGLE" => Some("ctrl-^".to_string()),
            _ => None,
        };
        let c = base.apply_env(env);
        assert_eq!(c.method, InputMethod::Telex); // env beat the file
        assert!(!c.enabled);
        assert_eq!(c.toggle, 0x1e); // '^' (0x5e) & 0x1f
    }

    #[test]
    fn env_ignores_unset_and_invalid() {
        let base = from_json(r#"{ "method": "telex" }"#);
        let c = base.clone().apply_env(|_| None);
        assert_eq!(c, base); // nothing set → unchanged
        let c = base.apply_env(|k| (k == "FUNPUT_METHOD").then(|| "garbage".to_string()));
        assert_eq!(c.method, InputMethod::Telex); // invalid value ignored
    }

    #[test]
    fn cycle_method_key_defaults_and_overrides() {
        assert_eq!(from_json("{}").cycle_method, DEFAULT_CYCLE_METHOD);
        // A custom key spec.
        let c =
            from_json("{}").apply_env(|k| (k == "FUNPUT_CYCLE_METHOD").then(|| "ctrl-]".into()));
        assert_eq!(c.cycle_method, Some(0x1d));
        // Disabled.
        let c = from_json("{}").apply_env(|k| (k == "FUNPUT_CYCLE_METHOD").then(|| "off".into()));
        assert_eq!(c.cycle_method, None);
    }

    #[test]
    fn toggle_spec_parsing() {
        assert_eq!(parse_toggle("ctrl-\\"), Some(0x1c));
        assert_eq!(parse_toggle("Ctrl-Space"), Some(0x00));
        assert_eq!(parse_toggle("c-]"), Some(0x1d));
        assert_eq!(parse_toggle("nope"), None);
    }
}
