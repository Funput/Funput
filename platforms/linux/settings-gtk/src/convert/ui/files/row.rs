//! One file's row in the batch list.
//!
//! Built fresh on every refresh rather than recycled. That is the whole reason this
//! list is a `ListBox` and not a `ColumnView` — see the module doc next door.

use std::rc::Rc;

use adw::prelude::*;

use crate::convert::state;
use crate::convert::ui::widget;
use crate::convert::Convert;

/// One file's row: what it is, what it will cost, and its own escape hatch.
pub(super) fn build(
    convert: &Rc<Convert>,
    index: usize,
    entry: &funput_convert::Entry,
) -> adw::ActionRow {
    let picker = gtk::DropDown::from_strings(&state::names());
    picker.set_valign(gtk::Align::Center);
    widget::select(&picker, entry.charset.and_then(state::index_of));
    widget::connect_dropdown(&picker, convert, move |convert, picked| {
        convert.state.borrow_mut().pick_file_source(index, picked);
    });

    let row = adw::ActionRow::builder().title(entry.name()).build();
    row.set_title_lines(1);
    if entry.unmapped > 0 {
        row.set_subtitle(&format!("{} chữ sẽ mất", entry.unmapped));
        row.add_css_class("warning");
    }
    row.add_suffix(&picker);
    row
}
