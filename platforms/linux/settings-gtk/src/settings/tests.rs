use super::{ExtraOnsetLetters, OnsetLetter, Settings, ToneStyle};

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
fn a_leftover_non_preedit_key_is_read_and_not_saved_back() {
    // Direct typing used to be a switch, so an older settings.json still carries the
    // key. It has to load, and the next save must not write it again.
    let json = LEGACY.replace('{', "{\n    \"nonPreedit\":false,");
    let settings: Settings = serde_json::from_str(&json).unwrap();
    assert!(settings.has_completed_onboarding); // the rest of the document was read
    let saved = serde_json::to_value(&settings).unwrap();
    assert!(saved.get("nonPreedit").is_none());
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

#[test]
fn a_document_without_extra_onsets_admits_none() {
    let settings: Settings = serde_json::from_str(LEGACY).unwrap();
    assert!(settings.extra_onsets.is_empty());
    assert!(Settings::default().extra_onsets.is_empty());
}

#[test]
fn extra_onsets_round_trip_spelled_out() {
    let settings = Settings {
        extra_onsets: ExtraOnsetLetters::NONE
            .with(OnsetLetter::J, true)
            .with(OnsetLetter::Z, true),
        ..Settings::default()
    };
    let json = serde_json::to_value(&settings).unwrap();
    // The same key and spelling the addon, Windows and the export format read.
    assert_eq!(json["extraOnsets"], "zj");
    let back: Settings = serde_json::from_value(json).unwrap();
    assert_eq!(back.extra_onsets, settings.extra_onsets);
}

#[test]
fn extra_onsets_read_any_case_and_skip_unknown_letters() {
    let json = LEGACY.replace('{', "{\n    \"extraOnsets\":\"zxQ\",");
    let settings: Settings = serde_json::from_str(&json).unwrap();
    assert_eq!(settings.extra_onsets, ExtraOnsetLetters::from_id("z"));
}

#[test]
fn a_non_string_extra_onsets_does_not_cost_the_rest_of_the_file() {
    for value in ["7", "null", "[\"z\"]"] {
        let json = LEGACY.replace('{', &format!("{{\n    \"extraOnsets\":{value},"));
        let settings: Settings = serde_json::from_str(&json).unwrap();
        assert!(settings.extra_onsets.is_empty(), "{value}");
        assert!(settings.has_completed_onboarding, "{value}"); // the rest was read
    }
}
