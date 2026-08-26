//! Filling the text pane from the state.
//!
//! Every property this pane owns is set on every refresh, unconditionally. With
//! three shapes and two charsets in play, setting them one at a time is how a window
//! ends up showing a target it is no longer converting to.

use std::rc::Rc;

use adw::prelude::*;

use funput_convert::charset;

use crate::convert::state::{self, State};
use crate::convert::ui::widget;
use crate::convert::Convert;

use super::Pane;

impl Pane {
    pub(in crate::convert) fn refresh(&self, convert: &Rc<Convert>, state: &mut State) {
        let from_file = !state.files.is_empty();
        let source = match state.files.first() {
            Some(only) => only.charset.and_then(state::index_of),
            None => state.resolve_source(),
        };

        // Stated, not asked — until nothing can state it, and then it has to ask.
        self.source_label.set_label(if source.is_some() {
            "Đây là"
        } else {
            "Chưa đoán được — chọn bảng mã:"
        });
        self.source_label.set_css_classes(if source.is_some() {
            &["dim-label"]
        } else {
            &["warning"]
        });
        widget::select(&self.source, source);
        widget::select(&self.target, Some(state.target));

        // A file is a document being converted, not a draft being written.
        self.input.set_editable(!from_file);

        let (shown, converted, loss) = views(state);
        self.show_input(&shown);
        self.output.buffer().set_text(&converted);
        self.loss.set_label(&loss);
        self.loss.set_visible(!loss.is_empty());
        self.footer.refresh(convert, state, from_file);
    }

    /// Write the input pane without moving the caret.
    ///
    /// Rewriting a buffer that already holds this text sends the cursor home, so
    /// every keystroke would bounce the caret to position zero. Not writing at all is
    /// the fix; the *other* hazard — this write being read as typing — is settled by
    /// [`Convert::is_refreshing`], because it bites four different signals.
    fn show_input(&self, text: &str) {
        let buffer = self.input.buffer();
        let (start, end) = buffer.bounds();
        if buffer.text(&start, &end, false) != text {
            buffer.set_text(text);
        }
    }
}

/// What the two panes and the warning line show for the current state.
///
/// **Nothing is converted under a guess.** With no source settled the right pane
/// shows the document back rather than a conversion, and the warning stays quiet
/// until the picker is used — a wrong guess dressed up as a result is exactly what
/// this window exists to prevent.
///
/// One `read`, one `render`. The right pane is `render`'s own text, which for a
/// legacy target is its bytes read back — so what is on screen is what the file will
/// hold, `?` and all, rather than a second conversion that happens to look similar.
///
/// The panes are capped; the conversion and every counter are not. A long document
/// cannot make the window crawl, and the numbers stay honest.
fn views(state: &State) -> (String, String, String) {
    let Some((text, from, to)) = state.conversion() else {
        let text = state
            .files
            .first()
            .map_or(state.input.as_str(), |file| file.text.as_str());
        let shown = funput_convert::capped(text);
        return (shown.clone(), shown, String::new());
    };
    let rendered = charset::render(&charset::read(text, from), to);
    (
        funput_convert::capped(text),
        funput_convert::capped(&rendered.text),
        funput_convert::warning(&rendered.cost, from, to),
    )
}
