//! What sits right before the word being typed.

/// Longest run of symbols kept in front of a word, in bytes. A gõ tắt trigger may
/// open with this many (`;`, `;;`, `2`); a longer run still glues the word.
const RUN_CAP: usize = 8;

/// What sits right before the word being typed: whether it stands alone, and the
/// ASCII punctuation and digits glued to its front.
///
/// A word glued to text on screen is part of something larger: `vn` in `abc.vn`,
/// `t` in `don't`, `k` in `500k`. Gõ tắt leaves such a word alone, and a trigger
/// that opens with symbols (`;ct`) is looked up through [`Self::prefix`]. Only
/// whitespace, or the start of what the engine has seen, sets a word free; so does
/// a lone opener after either (`(vn`, `"vn`).
///
/// Unlike the rest of the per-word state, the run waiting for a word survives
/// `Session::clear()`: a host that resets the engine right after committing a
/// passed-through digit or a boundary (Android does) must still see `500k` glued.
/// It is forgotten when the host says the caret moved ([`Self::forget`]).
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub(crate) struct Lead {
    run: [u8; RUN_CAP],
    len: u8,
    flags: u8,
}

/// The run outgrew [`RUN_CAP`]: still glued, no longer a trigger prefix.
const OVERFLOW: u8 = 1;
/// Text, not whitespace, sits right before the run: a word just ended there.
const AFTER_TEXT: u8 = 1 << 1;
/// A word is being typed.
const LIVE: u8 = 1 << 2;
/// The live word is glued to what came before it.
const GLUED: u8 = 1 << 3;

impl Lead {
    /// Whether the live word is glued to the text before it.
    pub(crate) fn is_glued(&self) -> bool {
        self.has(LIVE | GLUED)
    }

    /// The symbols glued to the live word's front, when they fit a trigger prefix.
    pub(crate) fn prefix(&self) -> Option<&str> {
        let run = &self.run[..usize::from(self.len)];
        let fits = self.has(LIVE) && !run.is_empty() && self.flags & OVERFLOW == 0;
        fits.then(|| std::str::from_utf8(run).ok()).flatten()
    }

    /// Before the first key of a word lands. Settles whether the word is glued; the
    /// run stays as the word's prefix until the word ends.
    pub(crate) fn start_word(&mut self) {
        let run = &self.run[..usize::from(self.len)];
        let free = self.flags & (AFTER_TEXT | OVERFLOW) == 0
            && (run.is_empty() || run.len() == 1 && is_opener(run[0]));
        self.flags |= LIVE;
        self.set(GLUED, !free);
    }

    /// After every key. `ended_word` says the key closed a word that was open before
    /// it; `word_open` that a word is open after it.
    pub(crate) fn after_key(&mut self, key: char, ended_word: bool, word_open: bool) {
        if word_open {
            return;
        }
        if ended_word {
            self.follow_text();
        }
        if key.is_whitespace() {
            self.forget();
        } else if key.is_ascii_punctuation() || key.is_ascii_digit() {
            self.push(key as u8);
        } else {
            self.follow_text();
        }
    }

    /// The word is gone: committed, abandoned, or reset by the host. What comes
    /// next is unknown, so it starts free; a run still waiting for its word stays.
    pub(crate) fn end_word(&mut self) {
        if self.has(LIVE) {
            *self = Self::default();
        }
    }

    /// The caret moved somewhere the keys typed so far no longer describe, or
    /// whitespace was typed: the next word starts free. A live word keeps its verdict.
    pub(crate) fn forget(&mut self) {
        let live = self.flags & (LIVE | GLUED);
        *self = Self::default();
        self.flags = live;
    }

    /// The next word starts right behind the word just ended: a sealed word.
    pub(crate) fn follow_text(&mut self) {
        self.len = 0;
        self.flags = AFTER_TEXT;
    }

    /// A committed word re-opened by the host, which saw `before` in front of it
    /// (`None`: nothing, or nothing it could read). An unknown front leaves the word
    /// free, as it was before the host could say; its prefix is never known.
    pub(crate) fn reopen(&mut self, before: Option<char>) {
        let free = before.is_none_or(|c| c.is_whitespace() || c.is_ascii() && is_opener(c as u8));
        *self = Self::default();
        self.flags = LIVE;
        self.set(GLUED, !free);
    }

    fn push(&mut self, byte: u8) {
        match self.run.get_mut(usize::from(self.len)) {
            Some(slot) => {
                *slot = byte;
                self.len += 1;
            }
            None => self.flags |= OVERFLOW,
        }
    }

    fn has(&self, flags: u8) -> bool {
        self.flags & flags == flags
    }

    fn set(&mut self, flag: u8, on: bool) {
        if on {
            self.flags |= flag;
        } else {
            self.flags &= !flag;
        }
    }
}

/// A symbol that opens a quote or a bracket: a word right after it still stands
/// alone when nothing but whitespace comes before it.
fn is_opener(byte: u8) -> bool {
    matches!(byte, b'(' | b'[' | b'{' | b'"' | b'\'')
}

#[cfg(test)]
mod tests {
    use super::Lead;

    /// Type `text` as the engine would see it: letters open a word, anything else
    /// ends the one open. Returns the lead as it stands for the next word.
    fn after(text: &str) -> Lead {
        let mut lead = Lead::default();
        let mut open = false;
        for key in text.chars() {
            if key.is_alphabetic() {
                if !open {
                    lead.start_word();
                }
                open = true;
            } else {
                if open {
                    lead.end_word();
                }
                lead.after_key(key, open, false);
                open = false;
            }
        }
        lead
    }

    fn glued(before: &str) -> bool {
        let mut lead = after(before);
        lead.start_word();
        lead.is_glued()
    }

    #[test]
    fn a_word_after_whitespace_or_nothing_stands_alone() {
        for before in ["", "abc ", "abc. ", "a\n", "500 "] {
            assert!(!glued(before), "{before:?}");
        }
    }

    #[test]
    fn a_word_after_a_lone_opener_stands_alone() {
        for before in ["(", "x (", "\"", "'", "[", "{"] {
            assert!(!glued(before), "{before:?}");
        }
    }

    #[test]
    fn a_word_glued_to_text_or_symbols_is_glued() {
        for before in ["abc.", "a@b.", "x/", "don'", "abc(", "500", "5.", "((", ";"] {
            assert!(glued(before), "{before:?}");
        }
    }

    #[test]
    fn the_symbols_in_front_are_the_prefix() {
        let mut lead = after("x ;;");
        lead.start_word();
        assert_eq!(lead.prefix(), Some(";;"));
        let mut long = after(" ;;;;;;;;;");
        long.start_word();
        assert!(long.is_glued());
        assert_eq!(long.prefix(), None, "longer than a trigger prefix");
    }

    #[test]
    fn a_word_with_nothing_in_front_has_no_prefix() {
        let mut lead = after("x ");
        lead.start_word();
        assert_eq!(lead.prefix(), None);
    }

    #[test]
    fn a_run_waiting_for_its_word_survives_a_host_reset() {
        let mut lead = after("500");
        lead.end_word();
        lead.start_word();
        assert!(lead.is_glued());
    }

    #[test]
    fn a_moved_caret_frees_the_next_word() {
        let mut lead = after("abc.");
        lead.forget();
        lead.start_word();
        assert!(!lead.is_glued());
    }

    #[test]
    fn a_sealed_word_glues_the_next_one() {
        let mut lead = Lead::default();
        lead.start_word();
        lead.end_word();
        lead.follow_text();
        lead.start_word();
        assert!(lead.is_glued());
    }

    #[test]
    fn a_reopened_word_is_glued_only_to_text_seen_in_front() {
        let reopened = |before| {
            let mut lead = Lead::default();
            lead.reopen(before);
            lead.is_glued()
        };
        assert!(!reopened(None));
        assert!(!reopened(Some(' ')));
        assert!(!reopened(Some('(')));
        assert!(reopened(Some('.')));
        assert!(reopened(Some('5')));
    }
}
