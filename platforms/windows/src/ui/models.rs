//! Adapters from persisted Rust data to Slint models.

use slint::{Model, ModelRc, SharedString, VecModel};

use crate::ShortcutEntry;
use funput_config::{FlipHotkey, Hotkey, KeyCombo};
use funput_desktop::{ListedShortcut, ShortcutNote};

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

/// The page's rows in the shell's listing order. Each entry keeps its stored
/// index: that, not the row's place on screen, is what an edit writes back to.
pub(super) fn shortcuts(listed: Vec<ListedShortcut>) -> ModelRc<ShortcutEntry> {
    let rows = listed
        .into_iter()
        .map(|row| {
            let mut entry = ShortcutEntry {
                trigger: row.shortcut.trigger.into(),
                expansion: row.shortcut.expansion.into(),
                index: i32::try_from(row.index).unwrap_or(i32::MAX),
                ..ShortcutEntry::default()
            };
            set_note(&mut entry, row.note);
            entry
        })
        .collect::<Vec<_>>();
    ModelRc::new(VecModel::from(rows))
}

/// The on-screen row that carries stored index `index`.
pub(super) fn row_for(model: &ModelRc<ShortcutEntry>, index: usize) -> Option<usize> {
    let index = i32::try_from(index).ok()?;
    (0..model.row_count()).find(|&row| {
        model
            .row_data(row)
            .is_some_and(|entry| entry.index == index)
    })
}

/// Put each note, by stored index, under the on-screen row that carries it. Rows
/// are updated in place, so the field being typed in keeps its focus.
pub(super) fn apply_notes(model: &ModelRc<ShortcutEntry>, mut notes: Vec<ShortcutNote>) {
    for row in 0..model.row_count() {
        let Some(mut entry) = model.row_data(row) else {
            continue;
        };
        let Some(note) = usize::try_from(entry.index)
            .ok()
            .and_then(|index| notes.get_mut(index))
        else {
            continue;
        };
        if set_note(&mut entry, std::mem::take(note)) {
            model.set_row_data(row, entry);
        }
    }
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
