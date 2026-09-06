//! Text state: what you have on the left, what it will become on the right.
//!
//! Also the shape a **single** dropped file takes. A batch needs a table because no
//! two rows are alike; one file has nothing to compare against, and a one-row table
//! hides the thing the user actually wants to see — whether the Vietnamese comes out
//! right. What differs is the footer, which is [`footer`]'s problem.
//!
//! The source is *stated*, not asked for — `Đây là` with the dropdown demoted to an
//! escape hatch. That inversion is the point of the whole window: the tool can read
//! the document, so it should not make the user guess first.
//!
//! Filling all of this from the state is [`show`]'s job.

mod footer;
mod show;

use std::rc::Rc;

use adw::prelude::*;

use crate::convert::Convert;
use crate::convert::ui::widget;

pub(in crate::convert) struct Pane {
    pub(in crate::convert) root: gtk::Box,
    source_label: gtk::Label,
    source: gtk::DropDown,
    target: gtk::DropDown,
    input: gtk::TextView,
    output: gtk::TextView,
    loss: gtk::Label,
    footer: footer::Footer,
}

impl Pane {
    pub(in crate::convert) fn new() -> Self {
        let names = funput_convert::charset_names();
        let source_label = gtk::Label::builder().css_classes(["dim-label"]).build();
        let source = gtk::DropDown::from_strings(&names);
        let target = gtk::DropDown::from_strings(&names);

        let head = gtk::Box::builder().spacing(8).build();
        head.append(&source_label);
        head.append(&source);
        head.append(&gtk::Box::builder().hexpand(true).build());
        head.append(&widget::caption("Sang"));
        head.append(&target);

        let (input, input_box) = widget::pane("Đang có");
        let (output, output_box) = widget::pane("Sẽ thành");
        output.set_editable(false);
        output.set_cursor_visible(false);

        let panes = gtk::Box::builder().spacing(12).vexpand(true).build();
        panes.append(&input_box);
        panes.append(&output_box);

        // Only shown when something will actually be lost. A line that shows every
        // time is a line people stop reading.
        let loss = gtk::Label::builder()
            .wrap(true)
            .xalign(0.0)
            .css_classes(["warning", "caption"])
            .build();

        let footer = footer::Footer::new();
        let root = gtk::Box::builder()
            .orientation(gtk::Orientation::Vertical)
            .spacing(12)
            .build();
        root.append(&head);
        root.append(&panes);
        root.append(&loss);
        root.append(&footer.root);

        Self {
            root,
            source_label,
            source,
            target,
            input,
            output,
            loss,
            footer,
        }
    }

    pub(in crate::convert) fn wire(&self, convert: &Rc<Convert>) {
        let weak = Rc::downgrade(convert);
        self.input.buffer().connect_changed(move |buffer| {
            let Some(convert) = weak.upgrade() else {
                return;
            };
            // A refresh writing into this buffer is not the user typing, and the
            // handler below treats every edit as a fresh paste — without this guard,
            // dropping a file would forget what the file turned out to be.
            if convert.is_refreshing() {
                return;
            }
            let (start, end) = buffer.bounds();
            let typed = buffer.text(&start, &end, false).to_string();
            convert.session.borrow_mut().set_input(typed);
            convert.refresh();
        });

        widget::connect_dropdown(&self.source, convert, |convert, index| {
            convert.session.borrow_mut().pick_source(Some(index));
        });
        widget::connect_dropdown(&self.target, convert, |convert, index| {
            convert.session.borrow_mut().set_target(index);
        });
        self.footer.wire(convert);
    }
}
