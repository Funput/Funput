//! Filling the text pane from the state.
//!
//! Every property this pane owns is set on every refresh, unconditionally. With
//! three shapes and two charsets in play, setting them one at a time is how a window
//! ends up showing a target it is no longer converting to.

use std::rc::Rc;

use adw::prelude::*;

use crate::convert::ui::widget;
use crate::convert::Convert;
use funput_convert::View;

use super::Pane;

impl Pane {
    pub(in crate::convert) fn refresh(&self, convert: &Rc<Convert>, view: &View) {
        // Stated, not asked — until nothing can state it, and then it has to ask.
        self.source_label.set_label(if view.source.is_some() {
            "Đây là"
        } else {
            "Chưa đoán được — chọn bảng mã:"
        });
        self.source_label.set_css_classes(if view.source.is_some() {
            &["dim-label"]
        } else {
            &["warning"]
        });
        widget::select(&self.source, view.source);
        widget::select(&self.target, Some(view.target));

        // A file is a document being converted, not a draft being written.
        self.input.set_editable(!view.from_file);
        if let Some(text) = &view.input_preview {
            self.show_input(text);
        }
        self.output.buffer().set_text(&view.output_preview);
        self.loss.set_label(&view.warning);
        self.loss.set_visible(!view.warning.is_empty());
        self.footer.refresh(convert, view);
    }

    /// Write the input pane without moving the caret.
    ///
    /// Called **only** when the view says the text is ours — a pasted paragraph
    /// belongs to the user, and writing it back on every redraw would send the caret
    /// home and, worse, read as a fresh paste that forgets the detected charset.
    /// Even for a file, rewriting a buffer that already holds this text moves the
    /// cursor, so an unchanged write is skipped.
    fn show_input(&self, text: &str) {
        let buffer = self.input.buffer();
        let (start, end) = buffer.bounds();
        if buffer.text(&start, &end, false) != text {
            buffer.set_text(text);
        }
    }
}
