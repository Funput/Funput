//! What the Gõ tắt page says under each row, and which rows reach the engine.
//!
//! A row is never dropped for what it says: a trigger that can never fire, or one
//! the table already holds, stays saved and on screen with a note under it, so a
//! table imported or written by an older build loses nothing. It only stays out of
//! the engine.

use funput_config::Shortcut;
use funput_core::shortcut::{TriggerOverlap, check_trigger, compare_triggers};

/// The note under one row. An error means the row never expands; a warning only
/// explains how it shares a word with another row.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct ShortcutNote {
    pub error: Option<String>,
    pub warning: Option<String>,
}

/// One note per row of `rows`, in order. A row still being typed (no trigger yet)
/// gets none.
pub fn shortcut_notes(rows: &[Shortcut]) -> Vec<ShortcutNote> {
    (0..rows.len()).map(|index| note(rows, index)).collect()
}

fn note(rows: &[Shortcut], index: usize) -> ShortcutNote {
    let trigger = rows[index].trigger.as_str();
    if trigger.is_empty() {
        return ShortcutNote::default();
    }
    if let Err(error) = check_trigger(trigger) {
        return ShortcutNote {
            error: Some(error.to_string()),
            warning: None,
        };
    }
    if repeats_an_earlier_row(rows, index) {
        return ShortcutNote {
            error: Some("Chữ tắt này đã có ở dòng trên, dòng này không bung.".into()),
            warning: None,
        };
    }
    let case_variant = rows.iter().enumerate().any(|(other, row)| {
        other != index && compare_triggers(&row.trigger, trigger) == TriggerOverlap::CaseVariant
    });
    ShortcutNote {
        error: None,
        warning: case_variant.then(|| {
            "Chỉ khác một dòng khác ở chữ hoa, chữ thường. Gõ đúng cách viết của dòng nào \
             thì dòng đó bung."
                .into()
        }),
    }
}

/// Whether row `index` is one the engine gets: a trigger that can fire, not
/// spelled exactly like an earlier row. The first row in the list wins. A new
/// row is inserted at the top, so a repeat of one already there takes its place
/// and the older row is the one that does not fire.
pub(in crate::shell) fn fires(rows: &[Shortcut], index: usize) -> bool {
    check_trigger(&rows[index].trigger).is_ok() && !repeats_an_earlier_row(rows, index)
}

fn repeats_an_earlier_row(rows: &[Shortcut], index: usize) -> bool {
    let trigger = &rows[index].trigger;
    rows[..index].iter().any(|row| &row.trigger == trigger)
}
