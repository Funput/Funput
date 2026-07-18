use super::*;
use crate::settings::{ExcludedApp, Hotkey, Method, Shortcut, ToneStyle};

#[test]
fn round_trip_preserves_state() {
    let source = Settings {
        method: Method::TelexAdvanced,
        tone_style: ToneStyle::Modern,
        toggle_hotkey: Hotkey::AltShift,
        shortcuts: vec![Shortcut {
            trigger: "vn".into(),
            expansion: "Việt Nam".into(),
        }],
        excluded_apps: vec![ExcludedApp {
            id: "code".into(),
            name: "VS Code".into(),
        }],
        ..Settings::default()
    };
    assert_eq!(
        serde_json::to_value(&source).unwrap()["method"],
        "telex_advanced"
    );
    let json = serde_json::to_string(&to_document(&source)).unwrap();
    let mut imported = Settings::default();
    apply(&mut imported, &serde_json::from_str(&json).unwrap());
    assert_eq!(imported.method, Method::TelexAdvanced);
    assert_eq!(imported.tone_style, ToneStyle::Modern);
    assert_eq!(imported.toggle_hotkey, Hotkey::AltShift);
    assert_eq!(imported.shortcuts.len(), 1);
    assert_eq!(imported.excluded_apps.len(), 1);
}

#[test]
fn merges_shortcuts_by_trigger() {
    let mut settings = Settings {
        shortcuts: vec![Shortcut {
            trigger: "vn".into(),
            expansion: "old".into(),
        }],
        ..Settings::default()
    };
    let doc = serde_json::from_str(
        r#"{"schema":"app.funput.config","version":1,"shortcuts":[{"trigger":"vn","expansion":"Việt Nam"},{"trigger":"kg","expansion":"không"}]}"#,
    )
    .unwrap();
    let summary = apply(&mut settings, &doc);
    assert_eq!((summary.shortcuts_added, summary.shortcuts_updated), (1, 1));
    assert_eq!(settings.shortcuts.len(), 2);
}

#[test]
fn imports_other_platforms_without_platform_state() {
    let files = [
        (
            r#"{"schema":"app.funput.config","version":1,"preferences":{"inputMethod":"vni","toneStyle":"modern"},"platform":{"macos":{"excludedApps":[]}}}"#,
            Method::Vni,
        ),
        (
            r#"{"schema":"app.funput.config","version":1,"preferences":{"inputMethod":"telex"},"platform":{"windows":{"toggleHotkey":"ctrl_space"}}}"#,
            Method::Telex,
        ),
    ];
    for (json, method) in files {
        let mut settings = Settings::default();
        let summary = apply(&mut settings, &serde_json::from_str(json).unwrap());
        assert_eq!(settings.method, method);
        assert!(!summary.applied_platform);
        assert!(settings.excluded_apps.is_empty());
    }
}
