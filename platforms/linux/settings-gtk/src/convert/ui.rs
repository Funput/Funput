//! The window's widget tree, and pushing the state into it.
//!
//! **The skeleton is built once and only ever has properties set on it.** Windows can
//! literally rebuild its window on every change because Slint properties are values;
//! recreating a `TextView` here would destroy focus, the caret and the selection on
//! every keystroke. What survives the translation is the discipline that matters:
//! every callback is *mutate the state, then refresh*, no callback touches a widget
//! directly, and [`Panes::refresh`] sets every property it owns rather than the ones
//! it thinks changed.
//!
//! Two places deviate, and each says why where it happens: the input buffer in
//! [`text::Pane`], and the batch list in [`files::Pane`], which is the closest thing
//! GTK has to Slint swapping a model.

mod empty;
mod files;
mod text;
mod widget;

use std::rc::Rc;

use crate::convert::Convert;
use funput_convert::{Mode, View};

pub(super) use files::ROWS;

/// The three shapes, and the stack that shows one of them.
pub(super) struct Panes {
    pub(super) stack: gtk::Stack,
    empty: empty::Pane,
    text: text::Pane,
    files: files::Pane,
}

impl Panes {
    pub(super) fn new() -> Self {
        let empty = empty::Pane::new();
        let text = text::Pane::new();
        let files = files::Pane::new();

        let stack = gtk::Stack::builder()
            .vexpand(true)
            .margin_top(18)
            .margin_bottom(18)
            .margin_start(18)
            .margin_end(18)
            .build();
        stack.add_named(&empty.root, Some("empty"));
        stack.add_named(&text.root, Some("text"));
        stack.add_named(&files.root, Some("files"));

        Self {
            stack,
            empty,
            text,
            files,
        }
    }

    pub(super) fn wire(&self, convert: &Rc<Convert>) {
        self.empty.wire(convert);
        self.text.wire(convert);
        self.files.wire(convert);
    }

    pub(super) fn refresh(&self, convert: &Rc<Convert>, view: &View) {
        match view.mode {
            Mode::Empty => self.stack.set_visible_child_name("empty"),
            // One shape, two sources. A pasted paragraph and a single file look the
            // same on screen; which one it is decides where the panes are filled
            // from, and the pane settles that for itself.
            Mode::Text => {
                self.text.refresh(convert, view);
                self.stack.set_visible_child_name("text");
            }
            Mode::Files => {
                self.files.refresh(convert, view);
                self.stack.set_visible_child_name("files");
            }
        }
    }
}
