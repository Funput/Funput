//! Whether the word being typed is glued to a number on screen.

/// Whether the text being typed is glued to a number on screen (`500` then `k`). A word
/// glued to one is a unit on that number, not a word of its own, so no gõ tắt trigger
/// matches it: `500k` must not become `500không`.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub(crate) enum NumberGlue {
    #[default]
    Loose,
    /// The last key was a digit that left nothing composing. Survives the end of a
    /// word: a word-start digit passes straight through to the app, and a host may
    /// reset the engine right after committing it (Android does), so this is the only
    /// trace of the number the next word starts against.
    Digit,
    /// The current word began against a number. Text a host inserts behind the
    /// engine's back (paste, emoji) can leave a word glued that really stands alone —
    /// a missed expansion, never a wrong one.
    Word,
}

impl NumberGlue {
    /// Before the first key of a word lands in `keys`. A word emptied by Backspace and
    /// retyped is still glued: the number is still on screen.
    pub(crate) fn start_word(&mut self) {
        if *self != Self::Loose {
            *self = Self::Word;
        }
    }

    /// After every composed key. A key that left no word open either passed a digit
    /// through (the next word will be glued) or ended the word (it will not).
    pub(crate) fn after_key(&mut self, key: char, word_open: bool) {
        if !word_open {
            *self = if key.is_ascii_digit() {
                Self::Digit
            } else {
                Self::Loose
            };
        }
    }

    /// The word is gone — committed, abandoned, or reset by the host. A pending digit
    /// is not a word, so it stays for the word that follows it.
    pub(crate) fn end_word(&mut self) {
        if *self == Self::Word {
            *self = Self::Loose;
        }
    }
}

#[cfg(test)]
mod tests {
    use super::NumberGlue;

    #[test]
    fn a_passed_through_digit_glues_the_next_word() {
        let mut glue = NumberGlue::Loose;
        glue.after_key('5', false);
        glue.end_word();
        glue.start_word();
        assert_eq!(glue, NumberGlue::Word);
    }

    #[test]
    fn a_boundary_frees_the_next_word() {
        let mut glue = NumberGlue::Word;
        glue.end_word();
        glue.after_key(' ', false);
        glue.start_word();
        assert_eq!(glue, NumberGlue::Loose);
    }

    #[test]
    fn keys_inside_a_word_leave_it_glued() {
        let mut glue = NumberGlue::Word;
        glue.after_key('5', true);
        glue.after_key('k', true);
        assert_eq!(glue, NumberGlue::Word);
    }
}
