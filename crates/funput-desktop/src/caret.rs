//! What a shell knows about the caret after it moved without typing.
//!
//! A hook shell sees keystrokes, never the document, so a caret that moves any
//! other way — a click, another app, an arrow key — lands somewhere the keys typed
//! so far no longer describe. Auto-capitalize is the rule that notices: its idea of
//! where the sentence stands is built from those keys, and a stale one capitalizes
//! the middle of a sentence (`Xong. `, click, `tiếp` → `Tiếp`).
//!
//! Every such move is reported as a [`Caret`] through
//! [`ShellState::caret_moved`](crate::ShellState::caret_moved), which also commits
//! the composition, so the two can never drift apart: a shell cannot drop the word
//! and forget to drop the sentence.

/// Where the caret landed, as far as the shell can tell.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Caret {
    /// At the start of a line: Enter, whose newline the app types and the engine
    /// never sees. The next letter opens a sentence.
    LineStart,
    /// Somewhere the shell cannot see. The next letter is left as typed until the
    /// keys typed from here end a sentence.
    ///
    /// Chosen over [`Caret::LineStart`] whenever the shell is guessing: a missed
    /// capital costs one Shift, a wrong one has to be deleted.
    Unknown,
}
