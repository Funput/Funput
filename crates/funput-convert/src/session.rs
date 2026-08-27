//! The window's state, and every decision made from it.
//!
//! **One home for eight rules that used to live in two.** Which shape the window is
//! in, which source charset wins, what a picker index means, what happens when an
//! index is out of range — all of it is here, and all of it is testable without a
//! display. A shell mutates, calls [`Session::refresh`], and draws
//! [`Session::view`]; it decides nothing.
//!
//! # Two calls, not one
//!
//! `refresh()` does the work and `view()` only reads. Reads borrow `&self`, so a
//! shell holding this in a `RefCell` takes the narrow borrow; the expensive moment
//! is named, so a shell can put it on whichever thread it uses (GTK and Slint do not
//! agree); and a C door maps to it one call at a time, because no borrow crosses
//! that boundary.
//!
//! The cost is that a mutation without a `refresh()` leaves a stale view. Both
//! shells already end every callback with a redraw, which is what makes that safe.
//!
//! Charsets are **indices into [`charset::ALL`]** — see [`index`].

mod act;
mod job;
mod query;
mod view;

use crate::batch::Entry;
use crate::{at, clamp, index_of};

pub use job::Job;
pub use view::{Mode, Row, Unreadable, View};

/// How many rows a shell is shown at once until it asks for more.
const WINDOW: usize = 500;

pub struct Session {
    pub(super) target: usize,
    pub(super) source: Option<usize>,
    pub(super) input: String,
    pub(super) files: Vec<Entry>,
    pub(super) unreadable: Vec<Unreadable>,
    pub(super) window: (usize, usize),
    view: View,
}

impl Default for Session {
    fn default() -> Self {
        Self::new()
    }
}

impl Session {
    pub fn new() -> Self {
        Self {
            // Unicode by default: converting *to* it is what nearly everyone opening
            // this window came to do.
            target: 0,
            source: None,
            input: String::new(),
            files: Vec::new(),
            unreadable: Vec::new(),
            window: (0, WINDOW),
            view: View::default(),
        }
    }

    /// A fresh paste is a fresh document: what the user chose for the last one says
    /// nothing about this one, so it is identified again.
    pub fn set_input(&mut self, text: String) {
        self.input = text;
        self.source = None;
    }

    /// Clamped on the way in, not on the way out: [`View::target`] is a position a
    /// shell feeds straight back into its own menu, so it has to be one that exists.
    /// A host storing the index across releases is exactly why `ALL` is append-only.
    pub fn set_target(&mut self, index: usize) {
        self.target = clamp(index);
    }

    /// The source picker was used.
    ///
    /// The same picker serves a pasted paragraph and a single file — they share the
    /// text shape on screen — so it has to land on whichever one is showing. Getting
    /// this wrong is invisible: it writes into the slot nobody can see.
    pub fn pick_source(&mut self, index: Option<usize>) {
        match self.files.as_mut_slice() {
            [only] => only.charset = index.map(at),
            _ => self.source = index.map(clamp),
        }
    }

    /// A row's own picker, in the batch shape. `row` counts from the whole batch,
    /// not from the window.
    pub fn pick_row_source(&mut self, row: usize, index: usize) {
        if let Some(entry) = self.files.get_mut(row) {
            entry.charset = Some(at(index));
        }
    }

    /// Take the result of a [`crate::scan`].
    ///
    /// Reading files is I/O and a session lives on the UI thread, so the crate hands
    /// the work out rather than doing it in place: a shell scans on whichever thread
    /// it uses and adopts the result here. That is also the only reason [`Scan`] is a
    /// type at all.
    pub fn adopt(&mut self, scan: crate::Scan) {
        self.input.clear();
        self.source = None;
        self.files = scan.entries;
        self.unreadable = scan.unreadable;
        self.window = (0, WINDOW);
    }

    pub fn set_row_window(&mut self, first: usize, len: usize) {
        self.window = (first, len);
    }

    pub fn reset(&mut self) {
        *self = Self::new();
    }

    /// Rebuild the view from the state. **The expensive call**: it converts the
    /// document, and every file in a batch, against the current target.
    pub fn refresh(&mut self) {
        self.resolve_source();
        self.view = view::build(self);
    }

    /// The view as of the last [`Session::refresh`]. Never computes.
    pub fn view(&self) -> &View {
        &self.view
    }
}

#[cfg(test)]
mod tests;
