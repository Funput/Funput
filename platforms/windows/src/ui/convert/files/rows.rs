//! Turning entries into what the window shows.
//!
//! Two shapes, because one file and many files are different questions. A batch needs
//! a table: the interesting thing is that the rows *differ*, each read on its own
//! terms. One file has nothing to compare against, so a one-row table would hide what
//! the user actually wants — whether the Vietnamese comes out right. It gets the same
//! before/after panes a pasted paragraph does.

use funput_convert::{
    capped,
    charset::{self, Charset},
    Entry,
};
use slint::{ModelRc, VecModel};

use crate::ui::convert::view;
use crate::{ConvertWindow, FileRow};

/// The batch: a row per file, and what the button will do.
pub(in crate::ui::convert) fn show_many(window: &ConvertWindow, entries: &[Entry]) {
    let rows: Vec<FileRow> = entries
        .iter()
        .map(|entry| FileRow {
            name: entry.name().into(),
            charset: entry.charset.map(Charset::name).unwrap_or_default().into(),
            index: entry
                .charset
                .and_then(view::index_of)
                .and_then(|i| i32::try_from(i).ok())
                .unwrap_or(-1),
            note: if entry.unmapped > 0 {
                format!("{} chữ sẽ mất", entry.unmapped).into()
            } else {
                slint::SharedString::new()
            },
        })
        .collect();
    window.set_files(ModelRc::new(VecModel::from(rows)));
    window.set_out_dir(funput_convert::out_dir_label(entries).into());

    let ready = funput_convert::ready(entries);
    window.set_can_convert(ready > 0);
    window.set_action_label(format!("Chuyển {ready} tệp").into());
}

/// One file, shown the way a pasted paragraph is.
pub(in crate::ui::convert) fn show_one(window: &ConvertWindow, entry: &Entry, target: Charset) {
    window.set_from_file(true);
    window.set_file_name(entry.name().into());
    window.set_source_index(
        entry
            .charset
            .and_then(view::index_of)
            .and_then(|i| i32::try_from(i).ok())
            .unwrap_or(-1),
    );
    window.set_input_text(capped(&entry.text).into());

    let Some(from) = entry.charset else {
        // Nothing explained it: show the document back rather than convert it under
        // a guess, and wait for the picker.
        window.set_output_text(capped(&entry.text).into());
        window.set_loss(slint::SharedString::new());
        return;
    };
    let rendered = charset::render(&charset::read(&entry.text, from), target);
    window.set_output_text(capped(&rendered.text).into());
    window.set_loss(funput_convert::warning(&rendered.cost, from, target).into());
}
