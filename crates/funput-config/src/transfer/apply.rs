//! Building a document from settings, and merging one back in.
//!
//! Import is **non-destructive**: it overwrites the fields the file carries and
//! leaves everything else alone. Nothing is ever deleted — an absent field means
//! "no opinion", not "clear it". That rule is what makes importing a partial or
//! foreign file safe, so each branch below is a plain `if let Some(..)`.

use crate::settings::{FlipHotkey, Hotkey, Method, Settings, Shortcut, ToneStyle};

use super::document::{
    CURRENT_VERSION, ConfigDocument, Platform, PortableShortcut, Preferences, SCHEMA_ID, Source,
    WindowsBlock,
};
use super::error::ImportSummary;
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

/// Merge `doc` into `s`, reporting what changed.
pub fn apply(s: &mut Settings, doc: &ConfigDocument) -> ImportSummary {
    let mut summary = ImportSummary {
        newer_version: doc.version > CURRENT_VERSION,
        ..Default::default()
    };
    if let Some(prefs) = &doc.preferences {
        apply_preferences(s, prefs);
    }
    if let Some(incoming) = &doc.shortcuts {
        merge_shortcuts(s, incoming, &mut summary);
    }
    if let Some(win) = doc.platform.as_ref().and_then(|p| p.windows.as_ref()) {
        apply_windows(s, win);
        summary.applied_platform = true;
    }
    summary
}

/// Unrecognised enum values leave the local preference untouched — a file from a
/// build with an input method this one lacks must not reset the user's choice.
fn apply_preferences(s: &mut Settings, prefs: &Preferences) {
    if let Some(m) = prefs.input_method.as_deref().and_then(Method::from_id) {
        s.method = m;
    }
    if let Some(t) = prefs.tone_style.as_deref().and_then(ToneStyle::from_id) {
        s.tone_style = t;
    }
    if let Some(v) = prefs.smart_english_restore {
        s.smart_restore = v;
    }
    if let Some(v) = prefs.eager_restore {
        s.eager_restore = v;
    }
    if let Some(v) = prefs.spell_check {
        s.spell_check = v;
    }
    if let Some(v) = prefs.auto_capitalize {
        s.auto_capitalize = v;
    }
}

/// Merge by `trigger`: a matching trigger takes the incoming expansion, a new one
/// is appended. Existing triggers absent from the file survive.
fn merge_shortcuts(s: &mut Settings, incoming: &[PortableShortcut], summary: &mut ImportSummary) {
    for item in incoming {
        if let Some(existing) = s.shortcuts.iter_mut().find(|sc| sc.trigger == item.trigger) {
            if existing.expansion != item.expansion {
                existing.expansion = item.expansion.clone();
                summary.shortcuts_updated += 1;
            }
        } else {
            s.shortcuts.push(Shortcut {
                trigger: item.trigger.clone(),
                expansion: item.expansion.clone(),
            });
            summary.shortcuts_added += 1;
        }
    }
}

fn apply_windows(s: &mut Settings, win: &WindowsBlock) {
    if let Some(h) = win.toggle_hotkey.as_deref().and_then(Hotkey::from_id) {
        s.toggle_hotkey = h;
    }
    if let Some(h) = win.flip_hotkey.as_deref().and_then(FlipHotkey::from_id) {
        s.flip_hotkey = h;
    }
    // Combos ride along explicitly: a file with a recorded combo restores it (and
    // it overrides the preset, matching the live settings semantics). A file
    // without combos leaves any locally recorded one untouched.
    if let Some(combo) = &win.toggle_combo {
        s.toggle_combo = Some(combo.clone());
    }
    if let Some(combo) = &win.flip_combo {
        s.flip_combo = Some(combo.clone());
    }
    // Both app sources merge by key with the local entry winning, so importing a
    // colleague's file never rewrites a choice this machine's user made. Newer
    // exports carry the memory directly; older ones only have the removed
    // exclusion list, whose ids mean "remembered as English".
    if let Some(memory) = &win.app_language_memory {
        for (id, &on) in memory {
            if !id.is_empty() {
                s.app_language_memory.entry(id.clone()).or_insert(on);
            }
        }
    }
    if let Some(apps) = &win.excluded_apps {
        s.remember_as_english(apps.iter().map(|app| app.id.clone()));
    }
}
