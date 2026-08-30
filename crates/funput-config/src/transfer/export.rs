//! Building an interchange document from the current settings.
//!
//! The import half lives in [`super::apply`] — the two directions are separate
//! because only one of them has a rule to enforce (import never deletes).

use crate::settings::Settings;

use super::document::{
    CURRENT_VERSION, ConfigDocument, Platform, PortableShortcut, Preferences, SCHEMA_ID, Source,
    WindowsBlock,
};
use super::time::iso8601_now;

/// Snapshot `s` into an interchange document. `source` names the app that wrote
/// it — the caller supplies it because only the shell knows the app's version.
pub fn to_document(s: &Settings, source: Source) -> ConfigDocument {
    ConfigDocument {
        schema: SCHEMA_ID.to_string(),
        version: CURRENT_VERSION,
        exported_at: Some(iso8601_now()),
        source: Some(source),
        preferences: Some(Preferences {
            input_method: Some(s.method.id().to_string()),
            tone_style: Some(s.tone_style.id().to_string()),
            smart_english_restore: Some(s.smart_restore),
            eager_restore: Some(s.eager_restore),
            spell_check: Some(s.spell_check),
            auto_capitalize: Some(s.auto_capitalize),
            shortcuts_enabled: Some(s.shortcuts_enabled),
        }),
        shortcuts: Some(
            s.shortcuts
                .iter()
                .map(|sc| PortableShortcut {
                    trigger: sc.trigger.clone(),
                    expansion: sc.expansion.clone(),
                })
                .collect(),
        ),
        platform: Some(Platform {
            windows: Some(WindowsBlock {
                toggle_hotkey: Some(s.toggle_hotkey.id().to_string()),
                toggle_combo: s.toggle_combo.clone(),
                flip_hotkey: Some(s.flip_hotkey.id().to_string()),
                flip_combo: s.flip_combo.clone(),
                app_language_memory: Some(s.app_language_memory.clone()),
                excluded_apps: None,
            }),
        }),
    }
}
