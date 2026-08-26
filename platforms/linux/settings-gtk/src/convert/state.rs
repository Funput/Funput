//! The window's state, and every decision made from it.
//!
//! **This is the only file under `convert/` that holds a decision.** Which shape the
//! window is in, which source charset wins, what a picker index means, what happens
//! when an index is out of range — all of it lives here, so all of it is testable
//! without a display. The `ui/*` files read this and set properties; they decide
//! nothing.
//!
//! A charset is an **index into [`charset::ALL`]**, here and in every dropdown. No
//! file under `convert/` names a charset, so implementing VISCII in core lengthens
//! every menu in this window without a line changing in it.

use funput_convert::{
    charset::{self, Charset},
    Entry, Mode,
};

pub struct State {
    /// Index into [`charset::ALL`]. Unicode by default: converting *to* it is what
    /// nearly everyone opening this window came to do.
    pub target: usize,
    /// Text state. `None` until something is identified or the user picks.
    pub source: Option<usize>,
    pub input: String,
    pub files: Vec<Entry>,
}

impl Default for State {
    fn default() -> Self {
        Self::new()
    }
}

impl State {
    pub const fn new() -> Self {
        Self {
            target: 0,
            source: None,
            input: String::new(),
            files: Vec::new(),
        }
    }

    pub fn mode(&self) -> Mode {
        Mode::of(&self.files, &self.input)
    }

    /// The source charset for the pasted paragraph, identifying it if nothing has
    /// been settled yet.
    ///
    /// **The user's own choice outranks the guess**: they are looking at the document
    /// and the detector is looking at statistics. Caches the answer back into
    /// `source`, so the guess is made once per document rather than once per redraw.
    pub fn resolve_source(&mut self) -> Option<usize> {
        let source = self
            .source
            .map_or_else(|| charset::detect(&self.input).and_then(index_of), Some);
        self.source = source;
        source
    }

    /// A fresh paste is a fresh document: what the user chose for the last one says
    /// nothing about this one, so it is identified again.
    pub fn set_input(&mut self, text: String) {
        self.input = text;
        self.source = None;
    }

    /// The source picker was used.
    ///
    /// The same picker serves a pasted paragraph and a single file — they share the
    /// text shape on screen — so it has to land on whichever one is showing.
    pub fn pick_source(&mut self, picked: Option<usize>) {
        match self.files.as_mut_slice() {
            [only] => only.charset = picked.map(at),
            _ => self.source = picked,
        }
    }

    /// A row's own picker was used, in the batch shape.
    pub fn pick_file_source(&mut self, row: usize, picked: usize) {
        if let Some(entry) = self.files.get_mut(row) {
            entry.charset = Some(at(picked));
        }
    }

    /// The source and target the current content converts through, or `None` when
    /// nothing has explained it yet.
    pub fn conversion(&self) -> Option<(&str, Charset, Charset)> {
        let (from, text) = match self.files.as_slice() {
            [only] => (only.charset, only.text.as_str()),
            _ => (self.source.map(at), self.input.as_str()),
        };
        from.map(|from| (text, from, at(self.target)))
    }
}

/// The charset an index names, clamped.
///
/// An index can only come from a menu this window built out of `ALL`, so clamping
/// keeps a future mistake a wrong entry rather than a panic.
pub fn at(index: usize) -> Charset {
    charset::ALL[index.min(charset::ALL.len() - 1)]
}

pub fn index_of(charset: Charset) -> Option<usize> {
    charset::ALL.iter().position(|&c| c == charset)
}

/// The display names for every dropdown, in `ALL`'s order.
pub fn names() -> Vec<&'static str> {
    charset::ALL.iter().map(|c| c.name()).collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn entry(charset: Option<Charset>) -> Entry {
        Entry {
            path: std::path::PathBuf::from("a.txt"),
            text: "việt".to_string(),
            charset,
            unmapped: 0,
        }
    }

    /// The user is looking at the document; the detector is looking at statistics.
    #[test]
    fn a_charset_the_user_picked_outranks_the_detected_one() {
        let mut state = State::new();
        state.set_input("việt nam".to_string());
        state.pick_source(Some(1));

        assert_eq!(state.resolve_source(), Some(1));
    }

    /// What was chosen for the last document says nothing about this one.
    #[test]
    fn a_fresh_paste_forgets_the_charset_of_the_last_one() {
        let mut state = State::new();
        state.set_input("việt nam".to_string());
        state.pick_source(Some(2));
        state.set_input("hà nội".to_string());

        assert_eq!(state.source, None, "the last choice outlived its document");
    }

    /// A single file wears the text shape, so the one source picker on screen has to
    /// write into the file — not into the pasted-text slot nobody can see.
    #[test]
    fn the_source_picker_lands_on_the_single_file_when_one_is_open() {
        let mut state = State {
            files: vec![entry(None)],
            ..State::new()
        };
        state.pick_source(Some(1));

        assert_eq!(state.files[0].charset, Some(at(1)));
        assert_eq!(state.source, None, "it wrote into the wrong slot");
    }

    /// A longer menu than `ALL` can only come from a bug; it must land on a wrong
    /// entry rather than take the window down.
    #[test]
    fn an_index_from_a_longer_menu_is_clamped_rather_than_panicking() {
        assert_eq!(at(99), charset::ALL[charset::ALL.len() - 1]);
    }

    /// Two files are a table; one is not.
    #[test]
    fn the_content_decides_the_shape() {
        let mut state = State::new();
        assert_eq!(state.mode(), Mode::Empty);
        state.set_input("việt".to_string());
        assert_eq!(state.mode(), Mode::Text);
        state.files = vec![entry(None), entry(None)];
        assert_eq!(state.mode(), Mode::Files);
    }
}
