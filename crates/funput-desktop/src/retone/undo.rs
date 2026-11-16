//! Keeping the shadow true when Backspace takes back a gõ tắt expansion.

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
}
