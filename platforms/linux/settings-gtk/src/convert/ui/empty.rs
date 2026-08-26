//! The starting state: the drop zone, and the two ways in without dragging.
//!
//! Files arrive by drop, which is registered on the window rather than here — see
//! [`crate::convert::io`]. Nothing in this pane listens for one; this is only what
//! the user is told and the two buttons that do the same thing by hand.

use std::rc::Rc;

use adw::prelude::*;

use crate::convert::{io, Convert};

pub(in crate::convert) struct Pane {
    pub(in crate::convert) root: adw::StatusPage,
    paste: gtk::Button,
    pick: gtk::Button,
}

impl Pane {
    pub(in crate::convert) fn new() -> Self {
        let paste = gtk::Button::builder()
            .label("Dán văn bản")
            .css_classes(["suggested-action", "pill"])
            .build();
        let pick = gtk::Button::builder()
            .label("Chọn tệp…")
            .css_classes(["pill"])
            .build();

        let buttons = gtk::Box::builder()
            .orientation(gtk::Orientation::Horizontal)
            .spacing(12)
            .halign(gtk::Align::Center)
            .build();
        buttons.append(&paste);
        buttons.append(&pick);

        // Worth saying out loud: the tool people are coming from takes one source
        // charset for a whole folder, so nobody expects a batch to be read file by
        // file. It is the reason to drag more than one thing in here.
        //
        // The folder name comes from `funput_convert::OUT_DIR` rather than being
        // spelled again — this sentence is a promise about where the file will be,
        // and the user goes looking there.
        let batch = gtk::Label::builder()
            .label(format!(
                "Hỗ trợ chuyển đổi nhiều tệp cùng lúc, tệp chuyển đổi xong sẽ được lưu \
                 vào thư mục {} — cùng cấp với tệp nguồn.",
                funput_convert::OUT_DIR
            ))
            .wrap(true)
            .justify(gtk::Justification::Center)
            .css_classes(["dim-label", "caption"])
            .build();

        let body = gtk::Box::builder()
            .orientation(gtk::Orientation::Vertical)
            .spacing(18)
            .build();
        body.append(&batch);
        body.append(&buttons);

        let root = adw::StatusPage::builder()
            .icon_name("document-edit-symbolic")
            .title("Thả tệp vào đây, hoặc dán văn bản")
            .description("Funput tự nhận ra bảng mã — không cần chọn nguồn.")
            .child(&body)
            .vexpand(true)
            .build();

        Self { root, paste, pick }
    }

    pub(in crate::convert) fn wire(&self, convert: &Rc<Convert>) {
        let weak = Rc::downgrade(convert);
        self.paste.connect_clicked(move |_| {
            if let Some(convert) = weak.upgrade() {
                io::paste(&convert);
            }
        });
        let weak = Rc::downgrade(convert);
        self.pick.connect_clicked(move |_| {
            if let Some(convert) = weak.upgrade() {
                io::pick_files(&convert);
            }
        });
    }
}
