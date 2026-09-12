//! Batch state: one row per file, each with the charset **it** turned out to be.
//!
//! This is where the tool beats the one it replaces. UniKey's file converter takes a
//! single source charset for a whole folder, so an archive holding documents from
//! different eras converts most of itself to nonsense. Here every file is read on its
//! own, and a file nothing explains is left alone with its own picker rather than
//! being swept along with the rest.
//!
//! A `ListBox` rebuilt from the state, not a `ColumnView`. `ColumnView` recycles its
//! rows, and a recycled row's dropdown fires `notify::selected` while it is being
//! bound — which writes a "choice" into whichever entry the row used to show. That is
//! a nasty bug to own, and the boxed list is what the rest of this app looks like.

mod row;
mod show;

use std::rc::Rc;

use adw::prelude::*;

use crate::convert::ui::{casing, widget};
use crate::convert::{Convert, io};

/// How many rows to build at once.
///
/// The number is the shell's call, not the crate's: a GTK `ListBox` builds every row
/// it is handed, while Slint's model virtualizes. The counts and the writing run over
/// the whole batch either way, so a capped list stays honest — it is rebuilding two
/// thousand rows on every target change that is visibly slow.
pub(in crate::convert) const ROWS: usize = 500;

pub(in crate::convert) struct Pane {
    pub(in crate::convert) root: gtk::Box,
    count: gtk::Label,
    target: gtk::DropDown,
    casing: casing::Bar,
    list: gtk::ListBox,
    progress: gtk::Label,
    action: gtk::Button,
}

impl Pane {
    pub(in crate::convert) fn new() -> Self {
        let count = gtk::Label::builder().css_classes(["dim-label"]).build();
        let target = gtk::DropDown::from_strings(&funput_convert::charset_names());

        let head = gtk::Box::builder().spacing(8).build();
        head.append(&count);
        head.append(&gtk::Box::builder().hexpand(true).build());
        head.append(&widget::caption("Sang"));
        head.append(&target);

        let casing = casing::Bar::new();
        let list = gtk::ListBox::builder()
            .selection_mode(gtk::SelectionMode::None)
            .css_classes(["boxed-list"])
            .build();
        let scroller = gtk::ScrolledWindow::builder()
            .child(&list)
            .vexpand(true)
            .build();

        let progress = widget::caption("");
        progress.set_hexpand(true);
        progress.set_ellipsize(gtk::pango::EllipsizeMode::Middle);
        let action = gtk::Button::builder()
            .css_classes(["suggested-action"])
            .build();

        let footer = gtk::Box::builder().spacing(8).build();
        footer.append(&progress);
        footer.append(&action);

        let root = gtk::Box::builder()
            .orientation(gtk::Orientation::Vertical)
            .spacing(12)
            .build();
        root.append(&head);
        // One transform applies to the whole batch, the way one target charset
        // does — so it sits in the header, not in every row.
        root.append(&casing.root);
        root.append(&scroller);
        root.append(&footer);

        Self {
            root,
            count,
            target,
            casing,
            list,
            progress,
            action,
        }
    }

    pub(in crate::convert) fn wire(&self, convert: &Rc<Convert>) {
        self.casing.wire(convert);
        widget::connect_dropdown(&self.target, convert, |convert, index| {
            convert.session.borrow_mut().set_target(index);
        });
        let weak = Rc::downgrade(convert);
        self.action.connect_clicked(move |_| {
            if let Some(convert) = weak.upgrade() {
                io::convert_files(&convert);
            }
        });
    }
}
