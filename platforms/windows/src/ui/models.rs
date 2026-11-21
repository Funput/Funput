//! Adapters from persisted Rust data to Slint models.

use slint::{ModelRc, SharedString, VecModel};

use crate::ShortcutEntry;
use funput_config::{FlipHotkey, Hotkey, KeyCombo, Shortcut};
use funput_desktop::{ShortcutNote, shortcut_notes};

pub(super) fn caps(hotkey: Hotkey) -> ModelRc<SharedString> {
    keycaps(hotkey.caps())
}

pub(super) fn flip_caps(hotkey: FlipHotkey) -> ModelRc<SharedString> {
    keycaps(hotkey.caps())
}

pub(super) fn combo_caps(combo: &KeyCombo) -> ModelRc<SharedString> {
    let rows: Vec<SharedString> = combo.caps().iter().map(|c| c.as_str().into()).collect();
    ModelRc::new(VecModel::from(rows))
}

fn keycaps(caps: &[&'static str]) -> ModelRc<SharedString> {
    let rows: Vec<SharedString> = caps.iter().map(|c| (*c).into()).collect();
    ModelRc::new(VecModel::from(rows))
}

pub(super) fn shortcuts(shortcuts: &[Shortcut]) -> ModelRc<ShortcutEntry> {
    let rows = shortcuts
        .iter()
        .zip(shortcut_notes(shortcuts))
        .map(|(shortcut, note)| {
            let mut entry = ShortcutEntry {
                trigger: shortcut.trigger.clone().into(),
                expansion: shortcut.expansion.clone().into(),
                ..ShortcutEntry::default()
            };
            set_note(&mut entry, note);
            entry
        })
        .collect::<Vec<_>>();
    ModelRc::new(VecModel::from(rows))
}

/// Put the note under a row into its Slint entry; `true` when that changed it.
pub(super) fn set_note(entry: &mut ShortcutEntry, note: ShortcutNote) -> bool {
    let error: SharedString = note.error.unwrap_or_default().into();
    let warning: SharedString = note.warning.unwrap_or_default().into();
    let changed = entry.error != error || entry.warning != warning;
    entry.error = error;
    entry.warning = warning;
    changed
}
