//! What the session answers about itself.
//!
//! Queries, not commands: nothing here changes what the window is converting, with
//! the one exception that says so in its name. They are `pub(super)` because they
//! are the rules [`view`](super::view) and [`act`](super::act) are built from —
//! a shell sees their results through [`View`](super::View), never the rules.

use funput_core::charset::{self, Charset};

use crate::{at, index_of};

use super::{Mode, Session};

impl Session {
    /// Which shape the content puts the window in. **The content decides, not a
    /// mode switch** — what the user put in already says whether this is a paragraph
    /// or a batch, so asking would be asking them to repeat themselves.
    pub(super) fn mode(&self) -> Mode {
        match self.files.len() {
            0 if self.input.is_empty() => Mode::Empty,
            0 | 1 => Mode::Text,
            _ => Mode::Files,
        }
    }

    /// The document on screen: a single file's text, or what was pasted.
    pub(super) fn text(&self) -> &str {
        self.files.first().map_or(&self.input, |file| &file.text)
    }

    /// The source charset as a menu position — the single file's own, or the
    /// paragraph's.
    pub(super) fn source_index(&self) -> Option<usize> {
        match self.files.as_slice() {
            [only] => only.charset.and_then(index_of),
            _ => self.source,
        }
    }

    /// **The user's own choice outranks the guess**: they are looking at the
    /// document and the detector is looking at statistics. Cached back, so the guess
    /// is made once per document rather than once per redraw.
    pub(super) fn resolve_source(&mut self) {
        if self.source.is_none() && self.files.is_empty() {
            self.source = charset::detect(&self.input).and_then(index_of);
        }
    }

    /// What the window is converting: the text, what it is, and what it is becoming.
    /// `None` until something has explained it.
    pub(super) fn conversion(&self) -> Option<(&str, Charset, Charset)> {
        let (from, text) = match self.files.as_slice() {
            [only] => (only.charset, only.text.as_str()),
            _ => (self.source.map(at), self.input.as_str()),
        };
        from.map(|from| (text, from, at(self.target)))
    }
}
