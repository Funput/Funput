//! Keeping the shadow true through the edits no keystroke carries: Backspace taking
//! back a gõ tắt expansion, and a word sealed where it stands.

use super::CommittedTail;

impl CommittedTail {
    /// Whether the text before the caret still ends with `written`, as far as the
    /// shadow knows. A `false` is not proof it is gone: the shadow may simply have
    /// dropped the front of a long expansion to stay bounded.
    pub fn ends_with(&self, written: &str) -> bool {
        self.tail.ends_with(written)
    }

    /// The app replaced `written` at the caret with `shown`. Returns whether the
    /// shadow could follow; on `false` it no longer matches the screen and is
    /// cleared, so nothing downstream acts on a word that is not there.
    pub fn rewrite_end(&mut self, written: &str, shown: &str) -> bool {
        if !self.ends_with(written) {
            self.clear();
            return false;
        }
        self.tail.truncate(self.tail.len() - written.len());
        self.tail.push_str(shown);
        self.trim();
        true
    }

    /// The word being composed ended as shown with no key behind it: a sealed word.
    /// It is on screen already, so it joins the shadow like any other typed text and
    /// Backspace can re-open it.
    pub fn commit(&mut self, word: &str) {
        self.tail.push_str(word);
        self.trim();
    }
}
