use super::{Settings, ToneStyle};

const LEGACY: &str = r#"{
    "method":"vni",
    "enabled":true,
    "smartRestore":true,
    "eagerRestore":true,
    "toggleHotkey":"ctrl_backtick",
    "launchAtLogin":false,
    "hasCompletedOnboarding":true
}"#;

#[test]
fn a_new_install_uses_modern_tone_placement() {
    assert_eq!(Settings::default().tone_style, ToneStyle::Modern);
}

#[test]
fn a_legacy_document_without_tone_style_stays_traditional() {
    let settings: Settings = serde_json::from_str(LEGACY).unwrap();
    assert_eq!(settings.tone_style, ToneStyle::Traditional);
}

#[test]
fn an_explicit_tone_style_always_wins() {
    for (value, expected) in [
        ("traditional", ToneStyle::Traditional),
        ("modern", ToneStyle::Modern),
    ] {
        let json = LEGACY.replace("\n}", &format!(",\n    \"toneStyle\":\"{value}\"\n}}"));
        let settings: Settings = serde_json::from_str(&json).unwrap();
        assert_eq!(settings.tone_style, expected);
    }
}
