//! The Chuyển mã window: paste a paragraph or drop files, and convert them.
//!
//! Framework-agnostic on purpose. Fcitx5 and IBus both depend on the
//! `funput-settings` package at the exact version, so both get this window without a
//! line of C++ changing — it never asks which shell is running, never reads
//! `settings.json`, and never talks to the engine.
//!
//! Everything about *what* a conversion is and costs lives in [`funput_convert`],
//! shared with the Slint window on Windows so the two cannot drift. What is here is
//! this platform's half of it.
//!
//! - [`ui`] — the widget tree, and pushing the view into it.
//! - [`io`] — drag-and-drop, the clipboard, the dialogs, and getting file work off
//!   the UI thread.
//! - [`open`] — building the window, and reusing the one already up.

mod io;
mod open;
mod ui;

use std::cell::{Cell, RefCell};
use std::rc::Rc;

use adw::prelude::*;
use funput_convert::{Mode, Session};

pub use open::present;

/// The window, its state, and the two latches that keep signals honest.
pub struct Convert {
    window: adw::ApplicationWindow,
    restart: gtk::Button,
    pub(crate) session: RefCell<Session>,
    panes: ui::Panes,
    /// Set while [`Self::refresh`] is writing into widgets.
    ///
    /// Every write it makes — a dropdown selection, a text buffer, a rebuilt row —
    /// fires the same signal a user would, and each of those handlers edits the state
    /// and refreshes again. That is a re-entrant `borrow_mut` on `state`, i.e. a
    /// panic, and where it does not panic it quietly rewrites what it just read.
    /// One latch, checked at the top of every handler, settles all of them.
    refreshing: Cell<bool>,
    /// Set while a batch is being written, so the button cannot start a second one.
    busy: Cell<bool>,
    /// The footer line: what just happened, or empty.
    progress: RefCell<String>,
}

impl Convert {
    /// Rebuild the window from the session. Every callback ends here.
    ///
    /// Two calls, because the crate splits them: `Session::refresh` does the work —
    /// converting the document, and every file in a batch, against the target of the
    /// moment — and `view()` only reads it back.
    pub(crate) fn refresh(self: &Rc<Self>) {
        if self.refreshing.replace(true) {
            return;
        }
        self.session.borrow_mut().refresh();
        let session = self.session.borrow();
        // Nothing to start over from when nothing has been put in yet.
        self.restart.set_visible(session.view().mode != Mode::Empty);
        self.panes.refresh(self, session.view());
        drop(session);
        self.refreshing.set(false);
    }

    pub(crate) fn window(&self) -> &adw::ApplicationWindow {
        &self.window
    }

    pub(crate) fn is_refreshing(&self) -> bool {
        self.refreshing.get()
    }

    pub(crate) fn is_busy(&self) -> bool {
        self.busy.get()
    }

    pub(crate) fn set_busy(&self, busy: bool) {
        self.busy.set(busy);
    }

    pub(crate) fn progress(&self) -> String {
        self.progress.borrow().clone()
    }

    pub(crate) fn set_progress(self: &Rc<Self>, message: String) {
        *self.progress.borrow_mut() = message;
        self.refresh();
    }
}
