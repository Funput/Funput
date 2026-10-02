//! The parts of an import that merge into what is already here instead of
//! replacing it: the gõ tắt table, matched by trigger, and the Linux-only block.

use crate::settings::{Settings, Shortcut};

use crate::config_transfer::document::{ConfigDocument, ImportSummary};
use crate::config_transfer::mapping::{flip_from_key, hotkey_from_key};

pub(super) fn merge_shortcuts(
    settings: &mut Settings,
    doc: &ConfigDocument,
    summary: &mut ImportSummary,
) {
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

pub(super) fn merge_linux(
    settings: &mut Settings,
    doc: &ConfigDocument,
    summary: &mut ImportSummary,
) {
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
    summary.applied_platform = true;
}
