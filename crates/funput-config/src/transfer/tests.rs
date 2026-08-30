use std::collections::BTreeMap;

use super::*;
use crate::settings::{FlipHotkey, Hotkey, KeyCombo, Method, Settings, Shortcut, ToneStyle};

fn source() -> Source {
    Source {
        platform: "windows".into(),
        app_version: "1.2026.1".into(),
    }
}

fn v_combo() -> KeyCombo {
    KeyCombo {
        vk: 0x56,
        ctrl: true,
        alt: false,
        shift: true,
        win: false,
        label: "V".into(),
    }
}

fn x_combo() -> KeyCombo {
    KeyCombo {
        vk: 0x58,
        ctrl: true,
        alt: true,
        shift: false,
        win: false,
        label: "X".into(),
    }
}

#[test]
fn round_trip_preserves_state() {
    let a = Settings {
        method: Method::Telex,
        tone_style: ToneStyle::Modern,
        toggle_hotkey: Hotkey::AltShift,
        toggle_combo: Some(v_combo()),
        flip_hotkey: FlipHotkey::CtrlShiftZ,
        flip_combo: Some(x_combo()),
        shortcuts: vec![Shortcut {
            trigger: "vn".into(),
            expansion: "Việt Nam".into(),
        }],
        app_language_memory: BTreeMap::from([("code.exe".to_string(), false)]),
        shortcut_smart_case: false,
        ..Settings::default()
    };

    let json = serde_json::to_string(&to_document(&a, source())).unwrap();
    let mut b = Settings::default();
    let doc: ConfigDocument = serde_json::from_str(&json).unwrap();
    apply(&mut b, &doc);

    assert_eq!(b.method, Method::Telex);
    assert_eq!(b.tone_style, ToneStyle::Modern);
    assert_eq!(b.toggle_hotkey, Hotkey::AltShift);
    assert_eq!(b.toggle_combo, Some(v_combo()));
    assert_eq!(b.flip_hotkey, FlipHotkey::CtrlShiftZ);
    assert_eq!(b.flip_combo, Some(x_combo()));
    assert_eq!(b.shortcuts.len(), 1);
    assert_eq!(b.shortcuts[0].expansion, "Việt Nam");
    assert_eq!(b.app_language_memory.get("code.exe"), Some(&false));
    assert!(!b.shortcut_smart_case, "both gõ tắt switches are portable");
    assert!(b.shortcuts_enabled);
}

/// Import never rewrites a choice this machine's user made — the same
/// "existing wins" rule the legacy migration uses.
#[test]
fn app_language_memory_merges_without_overwriting_a_local_choice() {
    let exported = Settings {
        app_language_memory: BTreeMap::from([
            ("code.exe".to_string(), false),
            ("chrome.exe".to_string(), false),
        ]),
        ..Settings::default()
    };
    let json = serde_json::to_string(&to_document(&exported, source())).unwrap();

    let mut local = Settings {
        app_language_memory: BTreeMap::from([("code.exe".to_string(), true)]),
        ..Settings::default()
    };
    let doc: ConfigDocument = serde_json::from_str(&json).unwrap();
    apply(&mut local, &doc);

    assert_eq!(local.app_language_memory.get("code.exe"), Some(&true));
    assert_eq!(local.app_language_memory.get("chrome.exe"), Some(&false));
}

/// A file exported before the per-app memory existed carries only the removed
/// exclusion list; each id has to arrive as "remembered as English".
#[test]
fn a_legacy_export_migrates_its_excluded_apps_to_english() {
    let legacy = r#"{
      "schema":"app.funput.config","version":1,
      "platform":{"windows":{"excludedApps":[{"id":"code.exe","name":"VS Code"}]}}
    }"#;
    let mut s = Settings::default();
    let doc: ConfigDocument = serde_json::from_str(legacy).unwrap();
    apply(&mut s, &doc);

    assert_eq!(s.app_language_memory.get("code.exe"), Some(&false));
}

/// The list is decode-only now: reading one must not put it back on the wire.
#[test]
fn export_no_longer_writes_the_legacy_exclusion_list() {
    let json = serde_json::to_string(&to_document(&Settings::default(), source())).unwrap();
    assert!(!json.contains("excludedApps"), "{json}");
    assert!(json.contains("appLanguageMemory"), "{json}");
}

/// `source` is metadata the caller owns. It must reach the file verbatim — the
/// app's version, never this crate's, which is why `to_document` takes it.
#[test]
fn source_is_written_as_given() {
    let json = serde_json::to_string(&to_document(&Settings::default(), source())).unwrap();
    assert!(json.contains("\"platform\":\"windows\""), "{json}");
    assert!(json.contains("\"appVersion\":\"1.2026.1\""), "{json}");
}

#[test]
fn export_writes_empty_combo_fields_as_null() {
    let json = serde_json::to_string(&to_document(&Settings::default(), source())).unwrap();
    assert!(json.contains("\"toggleCombo\":null"), "{json}");
    assert!(json.contains("\"flipCombo\":null"), "{json}");
}

#[test]
fn legacy_file_without_combos_keeps_local_combo() {
    // Pre-combo export (only preset ids): imports cleanly and does not wipe a
    // locally recorded combo (non-destructive merge).
    let legacy = r#"{"schema":"app.funput.config","version":1,
        "platform":{"windows":{"toggleHotkey":"ctrl_space","flipHotkey":"off"}}}"#;
    let mut s = Settings {
        toggle_combo: Some(v_combo()),
        ..Settings::default()
    };
    let doc: ConfigDocument = serde_json::from_str(legacy).unwrap();
    let summary = apply(&mut s, &doc);
    assert!(summary.applied_platform);
    assert_eq!(s.toggle_hotkey, Hotkey::CtrlSpace);
    assert_eq!(s.toggle_combo, Some(v_combo()));
}

#[test]
fn merges_shortcuts_by_trigger() {
    let mut s = Settings {
        shortcuts: vec![Shortcut {
            trigger: "vn".into(),
            expansion: "old".into(),
        }],
        ..Settings::default()
    };
    let doc: ConfigDocument = serde_json::from_str(
        r#"{"schema":"app.funput.config","version":1,"shortcuts":[{"trigger":"vn","expansion":"Việt Nam"},{"trigger":"kg","expansion":"không"}]}"#,
    )
    .unwrap();
    let summary = apply(&mut s, &doc);
    assert_eq!(summary.shortcuts_added, 1);
    assert_eq!(summary.shortcuts_updated, 1);
    assert_eq!(s.shortcuts.len(), 2);
    assert_eq!(
        s.shortcuts
            .iter()
            .find(|x| x.trigger == "vn")
            .unwrap()
            .expansion,
        "Việt Nam"
    );
}

#[test]
fn imports_macos_file_ignoring_its_platform_block() {
    // A macOS export: portable parts + platform.macos (KeyCombo) which we ignore.
    let macos = r#"{
      "schema":"app.funput.config","version":1,
      "preferences":{"inputMethod":"vni","toneStyle":"modern","smartEnglishRestore":false},
      "shortcuts":[{"trigger":"cf","expansion":"cà phê"}],
      "platform":{"macos":{"toggleShortcut":{"keyCode":42,"modifiers":262144,"label":"\\"},"excludedApps":[{"id":"com.apple.Safari","name":"Safari"}]}}
    }"#;
    let mut s = Settings::default();
    let doc: ConfigDocument = serde_json::from_str(macos).unwrap();
    let summary = apply(&mut s, &doc);
    assert_eq!(s.method, Method::Vni);
    assert_eq!(s.tone_style, ToneStyle::Modern);
    assert!(!s.smart_restore);
    assert_eq!(s.shortcuts.len(), 1);
    assert!(!summary.applied_platform); // no platform.windows → nothing OS-specific applied
    assert!(s.app_language_memory.is_empty()); // macOS bundleIds not imported
}

/// An enum value this build does not know must leave the local choice alone
/// rather than resetting it — the forward-compatibility rule in CONFIG_FORMAT.md.
#[test]
fn an_unknown_enum_value_leaves_the_local_preference() {
    let mut s = Settings {
        method: Method::Telex,
        ..Settings::default()
    };
    let doc: ConfigDocument = serde_json::from_str(
        r#"{"schema":"app.funput.config","version":1,"preferences":{"inputMethod":"vietnamese_2099"}}"#,
    )
    .unwrap();
    apply(&mut s, &doc);
    assert_eq!(s.method, Method::Telex);
}

#[test]
fn a_newer_version_still_imports_and_is_flagged() {
    let mut s = Settings::default();
    let doc: ConfigDocument = serde_json::from_str(
        r#"{"schema":"app.funput.config","version":99,"preferences":{"spellCheck":true}}"#,
    )
    .unwrap();
    let summary = apply(&mut s, &doc);
    assert!(summary.newer_version);
    assert!(s.spell_check, "known fields still apply");
}

#[test]
fn rejects_foreign_schema() {
    let doc: ConfigDocument =
        serde_json::from_str(r#"{"schema":"something.else","version":1}"#).unwrap();
    assert_ne!(doc.schema, SCHEMA_ID);
}
