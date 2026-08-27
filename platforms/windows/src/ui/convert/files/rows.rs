//! Turning the view's rows into Slint's model.
//!
//! Only the batch shape needs this. One file wears the text shape — it has nothing to
//! compare against, so a one-row table would hide the thing the user actually wants
//! to see — and [`super::super::view`] fills those panes straight from the view.

use funput_convert::View;
use slint::{ModelRc, VecModel};

use crate::ui::convert::view::index;
use crate::{ConvertWindow, FileRow};

/// The batch: a row per file, and what the button will do.
pub(in crate::ui::convert) fn show_many(window: &ConvertWindow, view: &View) {
    let names = funput_convert::charset_names();
    let rows: Vec<FileRow> = view
        .rows
        .iter()
        .map(|row| FileRow {
            name: row.name.clone().into(),
            charset: row
                .charset
                .and_then(|i| names.get(i))
                .copied()
                .unwrap_or_default()
                .into(),
            index: index(row.charset),
            note: row.note.clone().into(),
        })
        .collect();
    window.set_files(ModelRc::new(VecModel::from(rows)));

    // A file that could not be read is named rather than counted, in the footer that
    // otherwise promises where the copies will land.
    let unreadable = funput_convert::unreadable_line(&view.unreadable);
    if unreadable.is_empty() {
        window.set_out_dir(view.out_dir.clone().into());
    } else {
        window.set_out_dir(unreadable.into());
    }

    window.set_can_convert(view.ready > 0);
    window.set_action_label(format!("Chuyển {} tệp", view.ready).into());
}
