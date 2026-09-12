//! Filling the batch pane from the state.
//!
//! Split out of [`super`] for the same reason [`crate::convert::ui::text::show`] was:
//! the pane file holds the skeleton and the wiring, and the redraw is a separate
//! concern that grows on its own schedule. Batch mode had run out of room in one file
//! long before the second axis arrived.
//!
//! The list is rebuilt rather than updated in place — the reason is in [`super`]'s doc,
//! and it is the one place in this window that deviates from "set properties, never
//! build widgets" on purpose.

use std::rc::Rc;

use adw::prelude::*;

use crate::convert::Convert;
use crate::convert::ui::widget;
use funput_convert::View;

use super::{Pane, row};

impl Pane {
    pub(in crate::convert) fn refresh(&self, convert: &Rc<Convert>, view: &View) {
        self.count.set_label(&format!("{} tệp", view.rows_total));
        widget::select(&self.target, Some(view.target));

        while let Some(child) = self.list.first_child() {
            self.list.remove(&child);
        }
        for (offset, row) in view.rows.iter().enumerate() {
            self.list
                .append(&row::build(convert, view.rows_first + offset, row));
        }
        let shown = view.rows_first + view.rows.len();
        if shown < view.rows_total {
            let more = adw::ActionRow::builder()
                .title(format!("và {} tệp khác", view.rows_total - shown))
                .css_classes(["dim-label"])
                .build();
            self.list.append(&more);
        }

        // A file nothing explained is skipped, not guessed at, so the button counts
        // only what is settled — and says so, rather than promising the whole batch.
        self.action.set_label(&format!("Chuyển {} tệp", view.ready));
        self.action
            .set_sensitive(view.ready > 0 && !convert.is_busy());

        // Before a run, the footer is a promise about where the files will land; once
        // one has happened, it is the report. Never both, and never the promise after.
        //
        // A file that could not be read is *named* here rather than counted — a
        // number cannot answer "which two of my ten".
        let progress = convert.progress();
        let footer = if !progress.is_empty() {
            progress
        } else if view.unreadable.is_empty() {
            format!("Lưu vào: {}", view.out_dir)
        } else {
            funput_convert::unreadable_line(&view.unreadable)
        };
        self.progress.set_label(&footer);
    }
}
