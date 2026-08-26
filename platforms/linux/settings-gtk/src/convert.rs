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
//! - [`state`] — the state, and every decision made from it. The only pure file.
//! - [`ui`] — the widget tree, and pushing the state into it.
//! - [`io`] — drag-and-drop, the clipboard, the dialogs, and getting file work off
//!   the UI thread.
//! - [`open`] — building the window, and reusing the one already up.

mod io;
mod open;
mod state;
mod ui;

use std::cell::{Cell, RefCell};
use std::rc::Rc;

use adw::prelude::*;

use state::State;

pub use open::present;

/// The window, its state, and the two latches that keep signals honest.
pub struct Convert {
    window: adw::ApplicationWindow,
    restart: gtk::Button,
    pub(crate) state: RefCell<State>,
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
    /// Rebuild the window from the state. Every callback ends here.
    pub(crate) fn refresh(self: &Rc<Self>) {
        if self.refreshing.replace(true) {
            return;
        }
        let mut state = self.state.borrow_mut();
        // What each file would lose follows the target of the moment, not the target
        // it was read under, so it is remeasured here rather than at read time. It is
        // conversion only, no I/O, which is what makes that affordable.
        let target = state::at(state.target);
        funput_convert::measure(&mut state.files, target);
        // Nothing to start over from when nothing has been put in yet.
        self.restart
            .set_visible(state.mode() != funput_convert::Mode::Empty);
        self.panes.refresh(self, &mut state);
        drop(state);
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
