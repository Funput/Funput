//! The widget-building bits more than one pane needs.
//!
//! Nothing here decides anything — that is [`crate::convert::state`]'s job. These
//! only build, and set what they are told to set.

use std::rc::Rc;

use adw::prelude::*;

use crate::convert::Convert;

/// A titled `TextView` in a scroller, for one half of the before/after pair.
pub(in crate::convert) fn pane(title: &str) -> (gtk::TextView, gtk::Box) {
    let view = gtk::TextView::builder()
        .wrap_mode(gtk::WrapMode::WordChar)
        .left_margin(8)
        .right_margin(8)
        .top_margin(8)
        .bottom_margin(8)
        .build();
    let scroller = gtk::ScrolledWindow::builder()
        .child(&view)
        .vexpand(true)
        .has_frame(true)
        .build();
    let boxed = gtk::Box::builder()
        .orientation(gtk::Orientation::Vertical)
        .spacing(4)
        .hexpand(true)
        .build();
    boxed.append(&caption(title));
    boxed.append(&scroller);
    (view, boxed)
}

pub(in crate::convert) fn caption(text: &str) -> gtk::Label {
    gtk::Label::builder()
        .label(text)
        .xalign(0.0)
        .css_classes(["dim-label", "caption"])
        .build()
}

/// Point a dropdown at a charset index, or at nothing.
///
/// `INVALID_LIST_POSITION` is the GTK spelling of "nothing settled yet" — the same
/// thing the Windows window says with `-1`. Skipping an unchanged write keeps the
/// signal traffic down; the guard that actually matters is
/// [`Convert::is_refreshing`], because this write does fire `notify::selected`.
pub(in crate::convert) fn select(dropdown: &gtk::DropDown, index: Option<usize>) {
    let wanted = index.map_or(gtk::INVALID_LIST_POSITION, |i| i as u32);
    if dropdown.selected() != wanted {
        dropdown.set_selected(wanted);
    }
}

/// Connect a dropdown to a state edit.
///
/// Ignores the "no selection" position that [`select`] writes when nothing has been
/// settled — a window populating itself is not a user making a choice.
pub(in crate::convert) fn connect_dropdown(
    dropdown: &gtk::DropDown,
    convert: &Rc<Convert>,
    edit: impl Fn(&Rc<Convert>, usize) + 'static,
) {
    let weak = Rc::downgrade(convert);
    dropdown.connect_selected_notify(move |dropdown| {
        let Some(convert) = weak.upgrade() else {
            return;
        };
        if convert.is_refreshing() {
            return;
        }
        let selected = dropdown.selected();
        if selected == gtk::INVALID_LIST_POSITION {
            return;
        }
        edit(&convert, selected as usize);
        convert.refresh();
    });
}
