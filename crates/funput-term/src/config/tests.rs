use funput_core::{InputMethod, ToneStyle};

use super::{DEFAULT_TOGGLE, TermConfig, from_json};
use crate::terminal::DEFAULT_VI_CURSOR_COLOR;

#[test]
fn parses_canonical_camelcase_file() {
    let json = r#"{
        "method": "telex",
        "toneStyle": "modern",
        "enabled": true,
        "smartRestore": true,
        "eagerRestore": false,
        "spellCheck": true,
        "autoCapitalize": true,
        "shortcuts": [{ "trigger": "vn", "expansion": "Việt Nam" }]
    }"#;
    let c = from_json(json);
    assert_eq!(c.method, InputMethod::Telex);
    assert_eq!(c.tone_style, ToneStyle::Modern);
    assert!(c.enabled);
    assert!(!c.eager_restore);
    assert!(c.spell_check);
    assert!(c.auto_capitalize);
    assert_eq!(
        c.shortcuts,
        vec![("vn".to_string(), "Việt Nam".to_string())]
    );
}

#[test]
fn missing_keys_fall_back_to_defaults() {
    // Empty object: VNI, traditional, enabled, restore flags on, others off.
    let c = from_json("{}");
    assert_eq!(c, TermConfig::default());
    assert_eq!(c.method, InputMethod::Vni);
    assert_eq!(c.tone_style, ToneStyle::Traditional);
    assert!(c.enabled && c.smart_restore && c.eager_restore);
    assert!(!c.spell_check && !c.auto_capitalize);
    assert_eq!(c.toggle, DEFAULT_TOGGLE);
    assert_eq!(c.vi_cursor_color, DEFAULT_VI_CURSOR_COLOR);
}

#[test]
fn malformed_json_falls_back_to_defaults() {
    assert_eq!(from_json("not json"), TermConfig::default());
}
