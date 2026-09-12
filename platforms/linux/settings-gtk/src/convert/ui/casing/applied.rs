//! The second row: what is applied, the switches that belong to it, and the ways out.
//!
//! Shown and hidden as one unit, because with nothing applied there is no `Đang áp` line
//! to write, neither switch has a transform to belong to, and there is nothing to undo.
//! Same rule the loss warning follows one pane over: a row that is always there is a row
//! people stop reading.
//!
//! `gtk::Switch` with a label beside it, not `adw::SwitchRow`. A `SwitchRow` is an
//! `ActionRow` built to sit inside a `.boxed-list`; dropped into a horizontal bar it
//! renders as a full-width row with its own padding and background. The price of the
//! bare switch is that it carries no accessible name, so each one is pointed at its own
//! label — the one thing `SwitchRow` would have given for free.

use std::rc::Rc;

use adw::prelude::*;

use crate::convert::Convert;
use crate::convert::ui::widget;
use funput_convert::View;
use funput_core::textcase::Transform;

pub(super) struct Applied {
    pub(super) root: gtk::Box,
    line: gtk::Label,
    keep_d: Toggle,
    flatten_caps: Toggle,
    undo: gtk::Button,
    clear: gtk::Button,
}

/// A switch and the label that names it, shown and hidden together.
struct Toggle {
    root: gtk::Box,
    switch: gtk::Switch,
}

impl Applied {
    pub(super) fn new() -> Self {
        let line = widget::caption("");
        line.set_hexpand(true);
        line.set_ellipsize(gtk::pango::EllipsizeMode::End);

        // Each switch is named for what it turns *on*, so both read as off by default.
        // That is the crate's rule, not this window's, which is why neither is given a
        // starting value here — `refresh` takes both from the view.
        let keep_d = toggle("Giữ đ/Đ");
        let flatten_caps = toggle("Hạ chữ viết hoa");
        // Escape hatches, so no accent: `suggested-action` is spoken for by the one
        // action each pane exists to perform.
        let undo = gtk::Button::with_label("Hoàn tác");
        let clear = gtk::Button::with_label("Bỏ hết");

        let root = gtk::Box::builder().spacing(12).build();
        root.append(&line);
        root.append(&keep_d.root);
        root.append(&flatten_caps.root);
        root.append(&undo);
        root.append(&clear);

        Self {
            root,
            line,
            keep_d,
            flatten_caps,
            undo,
            clear,
        }
    }

    pub(super) fn wire(&self, convert: &Rc<Convert>) {
        widget::connect_switch(&self.keep_d.switch, convert, |convert, on| {
            convert.session.borrow_mut().set_keep_d(on);
        });
        widget::connect_switch(&self.flatten_caps.switch, convert, |convert, on| {
            convert.session.borrow_mut().set_flatten_caps(on);
        });
        // Undo removes the **last** transform, not all of them: a mistaken third press
        // should not cost the two before it. `Bỏ hết` is the one that goes back to the
        // original.
        widget::click(&self.undo, convert, |convert| {
            convert.session.borrow_mut().undo_transform();
            convert.refresh();
        });
        widget::click(&self.clear, convert, |convert| {
            convert.session.borrow_mut().clear_transforms();
            convert.refresh();
        });
    }

    pub(super) fn refresh(&self, view: &View, line: &str) {
        self.root.set_visible(!line.is_empty());
        self.line.set_label(line);
        // A switch is on screen only while the transform that owns it is applied. Any
        // other time it has nothing to say, and showing it only then is how the window
        // teaches which transform owns which.
        self.keep_d
            .refresh(super::applies(view, Transform::NoDiacritics), view.keep_d);
        self.flatten_caps
            .refresh(super::applies(view, Transform::Title), view.flatten_caps);
    }
}

impl Toggle {
    fn refresh(&self, shown: bool, on: bool) {
        self.root.set_visible(shown);
        self.switch.set_active(on);
    }
}

fn toggle(label: &str) -> Toggle {
    let caption = gtk::Label::builder()
        .label(label)
        .valign(gtk::Align::Center)
        .build();
    let switch = gtk::Switch::builder().valign(gtk::Align::Center).build();
    switch.update_relation(&[gtk::accessible::Relation::LabelledBy(&[
        caption.upcast_ref()
    ])]);

    let root = gtk::Box::builder().spacing(6).build();
    root.append(&caption);
    root.append(&switch);
    Toggle { root, switch }
}
