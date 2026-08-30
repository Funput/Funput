//! The on-disk contract. These guard the one failure nobody notices until a user
//! reports it: a rename or a dropped `#[serde(default)]` that silently resets a
//! setting the next time the app starts.

use super::*;

/// A `settings.json` with every field populated, exactly as the app writes it.
const FULL: &str = r#"{
  "method": "telex_advanced",
  "toneStyle": "modern",
  "enabled": false,
  "smartRestore": false,
  "eagerRestore": false,
  "spellCheck": true,
  "autoCapitalize": true,
  "toggleHotkey": "alt_shift",
  "toggleCombo": { "vk": 86, "ctrl": true, "alt": false, "shift": true, "win": false, "label": "V" },
  "flipHotkey": "ctrl_shift_z",
  "flipCombo": { "vk": 88, "ctrl": true, "alt": true, "shift": false, "win": false, "label": "X" },
  "launchAtLogin": true,
  "hasCompletedOnboarding": true,
  "appLanguageMemory": { "code.exe": false, "notepad.exe": true },
  "shortcuts": [{ "trigger": "vn", "expansion": "Việt Nam" }]
}"#;

#[test]
fn reads_every_field_of_a_full_file() {
    let s: Settings = serde_json::from_str(FULL).expect("full settings.json must decode");

    assert_eq!(s.method, Method::TelexAdvanced);
    assert_eq!(s.tone_style, ToneStyle::Modern);
    assert!(!s.enabled);
    assert!(!s.smart_restore);
    assert!(!s.eager_restore);
    assert!(s.spell_check);
    assert!(s.auto_capitalize);
    assert_eq!(s.toggle_hotkey, Hotkey::AltShift);
    assert_eq!(s.toggle_combo.as_ref().map(|c| c.vk), Some(86));
    assert_eq!(s.flip_hotkey, FlipHotkey::CtrlShiftZ);
    assert_eq!(s.flip_combo.as_ref().map(|c| c.label.as_str()), Some("X"));
    assert!(s.launch_at_login);
    assert!(s.has_completed_onboarding);
    assert_eq!(s.app_language_memory.get("code.exe"), Some(&false));
    assert_eq!(s.app_language_memory.get("notepad.exe"), Some(&true));
    assert_eq!(s.shortcuts[0].expansion, "Việt Nam");
}

#[test]
fn round_trips_through_json_unchanged() {
    let before: Settings = serde_json::from_str(FULL).unwrap();
    let json = serde_json::to_string(&before).unwrap();
    let after: Settings = serde_json::from_str(&json).unwrap();
    assert_eq!(before, after);
}

/// The camelCase keys are the contract — a file written by an older build must
/// keep loading, so the names are asserted literally rather than via round-trip.
#[test]
fn serializes_with_the_documented_key_names() {
    let json = serde_json::to_string(&Settings::default()).unwrap();
    for key in [
        "\"method\"",
        "\"toneStyle\"",
        "\"enabled\"",
        "\"smartRestore\"",
        "\"eagerRestore\"",
        "\"spellCheck\"",
        "\"autoCapitalize\"",
        "\"toggleHotkey\"",
        "\"flipHotkey\"",
        "\"launchAtLogin\"",
        "\"hasCompletedOnboarding\"",
        "\"appLanguageMemory\"",
        "\"shortcuts\"",
    ] {
        assert!(json.contains(key), "missing {key} in {json}");
    }
    // Absent combos are omitted, not written as null (`skip_serializing_if`).
    assert!(!json.contains("toggleCombo"), "{json}");
    assert!(!json.contains("flipCombo"), "{json}");
    // The exclusion list is decode-only now — reading it must not write it back.
    assert!(!json.contains("excludedApps"), "{json}");
}

/// Every optional field carries `#[serde(default)]`, so a file from a build that
/// predates it still loads instead of failing the whole read.
#[test]
fn a_minimal_legacy_file_falls_back_to_defaults() {
    let legacy = r#"{
      "method": "vni",
      "enabled": true,
      "smartRestore": true,
      "eagerRestore": true,
      "toggleHotkey": "ctrl_backtick",
      "launchAtLogin": false,
      "hasCompletedOnboarding": false
    }"#;
    let s: Settings = serde_json::from_str(legacy).expect("legacy settings.json must decode");

    assert_eq!(s.tone_style, ToneStyle::Traditional);
    assert!(!s.spell_check);
    assert!(!s.auto_capitalize);
    assert_eq!(s.flip_hotkey, FlipHotkey::Off);
    assert!(s.toggle_combo.is_none());
    assert!(s.app_language_memory.is_empty());
    assert!(s.shortcuts.is_empty());
}

/// The gõ tắt switch is the one added field whose absent value must not be `false`:
/// a file written before it existed belongs to someone with a working table, and
/// defaulting off would leave them wondering why their shortcuts died.
#[test]
fn a_file_without_the_shortcut_switch_still_expands() {
    let legacy = r#"{
      "method": "vni",
      "enabled": true,
      "smartRestore": true,
      "eagerRestore": true,
      "toggleHotkey": "ctrl_backtick",
      "launchAtLogin": false,
      "hasCompletedOnboarding": false,
      "shortcuts": [{ "trigger": "vn", "expansion": "Việt Nam" }]
    }"#;
    let s: Settings = serde_json::from_str(legacy).expect("legacy settings.json must decode");

    assert!(s.shortcuts_enabled);
    assert_eq!(s.shortcuts.len(), 1);
}

#[test]
fn the_shortcut_switch_round_trips_when_turned_off() {
    let s = Settings {
        shortcuts_enabled: false,
        ..Settings::default()
    };
    let text = serde_json::to_string(&s).expect("serialize");
    let back: Settings = serde_json::from_str(&text).expect("deserialize");
    assert!(!back.shortcuts_enabled);
}

/// Same rule, same reason: a file written before the smart-case switch existed comes
/// from someone whose triggers already match every capitalization. Defaulting off
/// would change what their table expands to, out of nowhere, on an update.
#[test]
fn a_file_without_the_smart_case_switch_still_matches_any_capitalization() {
    let legacy = r#"{
      "method": "vni",
      "enabled": true,
      "smartRestore": true,
      "eagerRestore": true,
      "toggleHotkey": "ctrl_backtick",
      "launchAtLogin": false,
      "hasCompletedOnboarding": false
    }"#;
    let s: Settings = serde_json::from_str(legacy).expect("legacy settings.json must decode");

    assert!(s.shortcut_smart_case);
}

#[test]
fn the_smart_case_switch_round_trips_when_turned_off() {
    let s = Settings {
        shortcut_smart_case: false,
        ..Settings::default()
    };
    let text = serde_json::to_string(&s).expect("serialize");
    let back: Settings = serde_json::from_str(&text).expect("deserialize");
    assert!(!back.shortcut_smart_case);
}

/// Same rule as the gõ tắt switch: absent means on. Someone who has been fighting
/// Funput inside a Japanese IME upgrades into the fix without going looking for it.
#[test]
fn a_file_without_the_foreign_layout_switch_still_auto_switches() {
    let legacy = r#"{
      "method": "vni",
      "enabled": true,
      "smartRestore": true,
      "eagerRestore": true,
      "toggleHotkey": "ctrl_backtick",
      "launchAtLogin": false,
      "hasCompletedOnboarding": false
    }"#;
    let s: Settings = serde_json::from_str(legacy).expect("legacy settings.json must decode");

    assert!(s.auto_english_on_foreign_layout);
}

#[test]
fn the_foreign_layout_switch_round_trips_when_turned_off() {
    let s = Settings {
        auto_english_on_foreign_layout: false,
        ..Settings::default()
    };
    let text = serde_json::to_string(&s).expect("serialize");
    let back: Settings = serde_json::from_str(&text).expect("deserialize");
    assert!(!back.auto_english_on_foreign_layout);
}
