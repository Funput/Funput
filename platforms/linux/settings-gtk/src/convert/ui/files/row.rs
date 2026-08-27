//! One file's row in the batch list.
//!
//! Built fresh on every refresh rather than recycled. That is the whole reason this
//! list is a `ListBox` and not a `ColumnView` — see the module doc next door.

use std::rc::Rc;

use adw::prelude::*;

use crate::convert::ui::widget;
use funput_convert::Row;

use crate::convert::Convert;

/// One file's row: what it is, what it will cost, and its own escape hatch.
pub(super) fn build(convert: &Rc<Convert>, index: usize, row: &Row) -> adw::ActionRow {
    let picker = gtk::DropDown::from_strings(&funput_convert::charset_names());
    picker.set_valign(gtk::Align::Center);
    widget::select(&picker, row.charset);
    widget::connect_dropdown(&picker, convert, move |convert, picked| {
        convert.session.borrow_mut().pick_row_source(index, picked);
    });

    let built = adw::ActionRow::builder().title(&row.name).build();
    built.set_title_lines(1);
    if !row.note.is_empty() {
        built.set_subtitle(&row.note);
        built.add_css_class("warning");
    }
    built.add_suffix(&picker);
    built
}
