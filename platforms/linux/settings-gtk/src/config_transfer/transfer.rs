use crate::settings::{Method, Settings, Shortcut};

use super::document::{
    ConfigDocument, ImportSummary, LinuxBlock, Platform, PortableShortcut, Preferences, Source,
    CURRENT_VERSION, SCHEMA_ID,
};
use super::mapping::{
    flip_from_key, flip_key, hotkey_from_key, hotkey_key, tone_from_key, tone_key,
};

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
                excluded_apps: Some(settings.excluded_apps.clone()),
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
    }
    merge_shortcuts(settings, doc, &mut summary);
    merge_linux(settings, doc, &mut summary);
    summary
}

fn merge_shortcuts(settings: &mut Settings, doc: &ConfigDocument, summary: &mut ImportSummary) {
    let Some(incoming) = &doc.shortcuts else {
        return;
    };
    for item in incoming {
        if let Some(existing) = settings
            .shortcuts
            .iter_mut()
            .find(|old| old.trigger == item.trigger)
        {
            if existing.expansion != item.expansion {
                existing.expansion.clone_from(&item.expansion);
                summary.shortcuts_updated += 1;
            }
        } else {
            settings.shortcuts.push(Shortcut {
                trigger: item.trigger.clone(),
                expansion: item.expansion.clone(),
            });
            summary.shortcuts_added += 1;
        }
    }
}

fn merge_linux(settings: &mut Settings, doc: &ConfigDocument, summary: &mut ImportSummary) {
    let Some(linux) = doc
        .platform
        .as_ref()
        .and_then(|platform| platform.linux.as_ref())
    else {
        return;
    };
    if let Some(hotkey) = linux.toggle_hotkey.as_deref().and_then(hotkey_from_key) {
        settings.toggle_hotkey = hotkey;
    }
    if let Some(hotkey) = linux.flip_hotkey.as_deref().and_then(flip_from_key) {
        settings.flip_hotkey = hotkey;
    }
    // Absent means "the exporter had nothing to say", not "turn it off" — the format's
    // non-destructive-import rule, and the difference between carrying a setting and
    // silently resetting it.
    if let Some(value) = linux.non_preedit {
        settings.non_preedit = value;
    }
    if let Some(apps) = &linux.excluded_apps {
        for app in apps {
            if !settings.excluded_apps.iter().any(|old| old.id == app.id) {
                settings.excluded_apps.push(app.clone());
            }
        }
    }
    summary.applied_platform = true;
}
