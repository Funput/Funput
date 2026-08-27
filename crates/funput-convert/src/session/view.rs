//! Everything the window shows, as plain owned values.
//!
//! **No field is derived and none holds a borrow.** That is not tidiness: it is what
//! lets a C door hand this over one accessor at a time, since no reference crosses
//! that boundary. Adding a method here that computes something would quietly make
//! the door stop being mechanical.
//!
//! Charsets are indices; paths are names. Both for the same reason.

use funput_core::charset;

use crate::batch;
use crate::text;

use super::{Session, at, index_of};

/// Which of the three shapes the window is in.
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub enum Mode {
    /// Nothing yet: the drop zone.
    #[default]
    Empty,
    /// A pasted paragraph — **or exactly one file**. One file has nothing to compare
    /// against, so a one-row table would hide the thing the user actually wants to
    /// see; it gets the before/after panes instead.
    Text,
    /// Two or more files: a table, because the interesting thing is that the rows
    /// differ.
    Files,
}

/// One file's row in the batch table.
#[non_exhaustive]
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct Row {
    pub name: String,
    /// Index into `charset::ALL`, or `None` when nothing explained the file.
    pub charset: Option<usize>,
    /// "N chữ sẽ mất", or empty.
    pub note: String,
}

/// A file that could not be read, and why.
///
/// **Named, not counted.** Ten files dropped and eight rows shown is a question the
/// user cannot answer from a number — and the reason separates "fix the permissions"
/// from "that file is not what you think it is".
#[non_exhaustive]
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Unreadable {
    pub name: String,
    pub reason: String,
}

#[non_exhaustive]
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct View {
    pub mode: Mode,
    pub target: usize,
    /// The source charset, stated rather than asked for. `None` means nothing could
    /// state it, and the shell must ask.
    pub source: Option<usize>,
    pub from_file: bool,
    pub file_name: Option<String>,
    /// The left pane — **`None` when the shell's own buffer is authoritative**.
    ///
    /// A pasted paragraph belongs to the user: writing it back on every redraw would
    /// send the caret home and, in GTK, read as a fresh paste that forgets the
    /// detected charset. A file's text belongs here, and is read-only on screen. Both
    /// shells had this rule as an ad-hoc latch; this is the rule itself.
    pub input_preview: Option<String>,
    pub output_preview: String,
    pub warning: String,
    /// The rows in the current window — see [`Session::set_row_window`].
    pub rows: Vec<Row>,
    pub rows_first: usize,
    pub rows_total: usize,
    pub out_dir: String,
    /// How many files a charset was settled for, over the **whole** batch.
    pub ready: usize,
    pub unreadable: Vec<Unreadable>,
}

pub(super) fn build(session: &Session) -> View {
    let target = at(session.target);
    let (text, rendered) = match session.conversion() {
        Some((text, from, to)) => (text, Some(charset::render(&charset::read(text, from), to))),
        None => (session.text(), None),
    };
    let single = session.files.len() == 1;
    View {
        mode: session.mode(),
        target: session.target,
        source: session.source_index(),
        from_file: single,
        file_name: single.then(|| session.files[0].name()),
        // A pasted paragraph is the shell's; a file's text is ours to show.
        input_preview: single.then(|| text::capped(text)),
        // With nothing settled, show the document back rather than convert it under
        // a guess — a wrong guess dressed up as a result is what this window exists
        // to prevent.
        output_preview: text::capped(rendered.as_ref().map_or(text, |r| &r.text)),
        warning: match (&rendered, session.conversion()) {
            (Some(r), Some((_, from, _))) => text::warning(&r.cost, from, target),
            _ => String::new(),
        },
        rows: rows(session, target),
        rows_first: session.window.0,
        rows_total: session.files.len(),
        out_dir: batch::out_dir_label(&session.files),
        ready: batch::ready(&session.files),
        unreadable: session.unreadable.clone(),
    }
}

/// Rows for the window only — but every count above runs over the whole batch, so a
/// capped list stays honest. Rebuilding two thousand of them on every target change
/// is what the window exists to avoid.
fn rows(session: &Session, target: charset::Charset) -> Vec<Row> {
    let (first, len) = session.window;
    session
        .files
        .iter()
        .skip(first)
        .take(len)
        .map(|entry| Row {
            name: entry.name(),
            charset: entry.charset.and_then(index_of),
            note: match entry.charset {
                Some(from) => {
                    let lost = charset::render(&charset::read(&entry.text, from), target)
                        .cost
                        .unrepresentable;
                    if lost > 0 {
                        format!("{lost} chữ sẽ mất")
                    } else {
                        String::new()
                    }
                }
                None => String::new(),
            },
        })
        .collect()
}
