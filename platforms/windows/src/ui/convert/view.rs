//! The window's session, and everything shown from it.
//!
//! [`refresh`] rebuilds the whole window, and every callback ends by calling it.
//! That is deliberate: with three shapes and two charsets in play, setting properties
//! one at a time is how a window ends up showing a target it is no longer converting
//! to.
//!
//! Nothing here decides anything. Which shape the window is in, which source charset
//! wins, what a picker index means — all of it lives in [`funput_convert::Session`],
//! shared with the GTK window on Linux, and arrives as a [`View`] of plain values.

use std::cell::RefCell;

use funput_convert::{Mode, Session, View};
use slint::Weak;

use crate::ConvertWindow;

use super::files;

thread_local! {
    pub(super) static WINDOW: RefCell<Option<Weak<ConvertWindow>>> = const { RefCell::new(None) };
    pub(super) static SESSION: RefCell<Session> = RefCell::new(Session::new());
}

pub(super) fn current() -> Option<ConvertWindow> {
    WINDOW.with(|cell| cell.borrow().as_ref().and_then(Weak::upgrade))
}

/// Edit the session, then redraw. The only way a callback touches anything.
pub(super) fn edit(change: impl FnOnce(&mut Session)) {
    SESSION.with(|s| change(&mut s.borrow_mut()));
    refresh();
}

/// Ask the session something without changing it.
pub(super) fn ask<R>(question: impl FnOnce(&Session) -> R) -> R {
    SESSION.with(|s| question(&s.borrow()))
}

/// Rebuild the whole window from the session.
///
/// Two calls, because the crate splits them: `Session::refresh` does the work —
/// converting the document, and every file in a batch, against the target of the
/// moment — and `view()` only reads it back.
pub(super) fn refresh() {
    let Some(window) = current() else { return };
    SESSION.with(|s| {
        let mut session = s.borrow_mut();
        session.refresh();
        show(&window, session.view());
    });
}

fn show(window: &ConvertWindow, view: &View) {
    window.set_target_index(i32::try_from(view.target).unwrap_or(0));
    window.set_source_index(index(view.source));
    window.set_from_file(view.from_file);
    window.set_file_name(view.file_name.clone().unwrap_or_default().into());
    window.set_output_text(view.output_preview.clone().into());
    window.set_loss(view.warning.clone().into());
    // Only when the view says the text is ours. A pasted paragraph belongs to the
    // user, and writing it back on every redraw would move the caret.
    if let Some(text) = &view.input_preview {
        window.set_input_text(text.clone().into());
    }

    match view.mode {
        Mode::Empty => window.set_mode("empty".into()),
        Mode::Text => window.set_mode("text".into()),
        Mode::Files => {
            files::show_many(window, view);
            window.set_mode("files".into());
        }
    }
}

/// A menu position as Slint spells it: `-1` for "nothing settled yet".
pub(super) fn index(position: Option<usize>) -> i32 {
    position.and_then(|i| i32::try_from(i).ok()).unwrap_or(-1)
}
