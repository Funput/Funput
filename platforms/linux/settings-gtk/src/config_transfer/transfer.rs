//! The settings ↔ document mapping. What an import *merges* rather than overwrites
//! (the gõ tắt table, the Linux block) lives in `transfer/merge.rs`.

mod merge;

use crate::settings::{ExtraOnsetLetters, Method, Settings};

use super::document::{
    CURRENT_VERSION, ConfigDocument, ImportSummary, LinuxBlock, Platform, PortableShortcut,
    Preferences, SCHEMA_ID, Source,
};
use super::mapping::{flip_key, hotkey_key, tone_from_key, tone_key};
use merge::{merge_linux, merge_shortcuts};

pub(super) fn to_document(settings: &Settings) -> ConfigDocument {
    ConfigDocument {
        schema: SCHEMA_ID.to_string(),
        version: CURRENT_VERSION,
        exported_at: Some(super::iso8601_now()),
        source: Some(Source {
            platform: "linux".to_string(),
            app_version: env!("CARGO_PKG_VERSION").to_string(),
        }),
        preferences: Some(Preferences {
            input_method: Some(settings.method.config_key().to_string()),
            tone_style: Some(tone_key(settings.tone_style).to_string()),
            smart_english_restore: Some(settings.smart_restore),
            eager_restore: Some(settings.eager_restore),
            spell_check: Some(settings.spell_check),
            auto_capitalize: Some(settings.auto_capitalize),
            extra_onsets: Some(settings.extra_onsets.id()),
            shortcuts_enabled: Some(settings.shortcuts_enabled),
            shortcut_smart_case: Some(settings.shortcut_smart_case),
            shortcuts_in_english: Some(settings.shortcuts_in_english),
        }),
        shortcuts: Some(
            settings
                .shortcuts
                .iter()
                .map(|item| PortableShortcut {
                    trigger: item.trigger.clone(),
                    expansion: item.expansion.clone(),
                })
                .collect(),
        ),
        platform: Some(Platform {
            linux: Some(LinuxBlock {
                toggle_hotkey: Some(hotkey_key(settings.toggle_hotkey).to_string()),
                flip_hotkey: Some(flip_key(settings.flip_hotkey).to_string()),
                non_preedit: Some(settings.non_preedit),
            }),
        }),
    }
}

pub(super) fn apply(settings: &mut Settings, doc: &ConfigDocument) -> ImportSummary {
    let mut summary = ImportSummary {
        newer_version: doc.version > CURRENT_VERSION,
        ..Default::default()
    };
    if let Some(prefs) = &doc.preferences {
        if let Some(method) = prefs
            .input_method
            .as_deref()
            .and_then(Method::from_config_key)
        {
            settings.method = method;
        }
        if let Some(tone) = prefs.tone_style.as_deref().and_then(tone_from_key) {
            settings.tone_style = tone;
        }
        if let Some(value) = prefs.smart_english_restore {
            settings.smart_restore = value;
        }
        if let Some(value) = prefs.eager_restore {
            settings.eager_restore = value;
        }
        if let Some(value) = prefs.spell_check {
            settings.spell_check = value;
        }
        if let Some(value) = prefs.auto_capitalize {
            settings.auto_capitalize = value;
        }
        // Absent keeps the local letters; present replaces them outright, unknown
        // letters skipped — a newer build's extra letter cannot cost the user the
        // ones known here.
        if let Some(letters) = prefs.extra_onsets.as_deref() {
            settings.extra_onsets = ExtraOnsetLetters::from_id(letters);
        }
        // Absent means the exporter had nothing to say, not "off" — the format's
        // non-destructive-import rule. A file predating these fields carries a
        // table that expands, smart-cased, and importing it must not change that.
        if let Some(value) = prefs.shortcuts_enabled {
            settings.shortcuts_enabled = value;
        }
        if let Some(value) = prefs.shortcut_smart_case {
            settings.shortcut_smart_case = value;
        }
        if let Some(value) = prefs.shortcuts_in_english {
            settings.shortcuts_in_english = value;
        }
    }
    merge_shortcuts(settings, doc, &mut summary);
    merge_linux(settings, doc, &mut summary);
    summary
}
