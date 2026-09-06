//! The text pane's footer: what happened, and what to do about it.
//!
//! Two primary buttons, never both. Pasted text goes through a Save dialog to a
//! path the user picks; a **dropped file** goes to the folder beside it under its own
//! name, the same way a batch does — no dialog, and never over the original.

use std::rc::Rc;

use adw::prelude::*;

use crate::convert::ui::widget;
use crate::convert::{Convert, io};
use funput_convert::View;

pub(in crate::convert) struct Footer {
    pub(in crate::convert) root: gtk::Box,
    progress: gtk::Label,
    copy: gtk::Button,
    save: gtk::Button,
    convert_file: gtk::Button,
}

impl Footer {
    pub(in crate::convert) fn new() -> Self {
        let progress = widget::caption("");
        progress.set_hexpand(true);
        progress.set_ellipsize(gtk::pango::EllipsizeMode::Middle);

        let copy = gtk::Button::with_label("Chép kết quả");
        let save = gtk::Button::builder()
            .label("Lưu tệp…")
            .css_classes(["suggested-action"])
            .build();
        let convert_file = gtk::Button::builder()
            .label("Chuyển tệp")
            .css_classes(["suggested-action"])
            .build();

        let root = gtk::Box::builder().spacing(8).build();
        root.append(&progress);
        root.append(&copy);
        root.append(&save);
        root.append(&convert_file);

        Self {
            root,
            progress,
            copy,
            save,
            convert_file,
        }
    }

    pub(in crate::convert) fn wire(&self, convert: &Rc<Convert>) {
        click(&self.copy, convert, io::copy_result);
        click(&self.save, convert, io::save_result);
        click(&self.convert_file, convert, io::convert_files);
    }

    pub(in crate::convert) fn refresh(&self, convert: &Rc<Convert>, view: &View) {
        self.save.set_visible(!view.from_file);
        self.convert_file.set_visible(view.from_file);
        // Nothing to copy or write until something has explained the document.
        let ready = view.source.is_some();

        self.copy.set_sensitive(ready);
        self.save.set_sensitive(ready);
        self.convert_file.set_sensitive(ready && !convert.is_busy());
        self.progress.set_label(&convert.progress());
    }
}

fn click(button: &gtk::Button, convert: &Rc<Convert>, action: fn(&Rc<Convert>)) {
    let weak = Rc::downgrade(convert);
    button.connect_clicked(move |_| {
        if let Some(convert) = weak.upgrade() {
            action(&convert);
        }
    });
}
