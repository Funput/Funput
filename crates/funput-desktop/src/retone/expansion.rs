//! Where the last gõ tắt expansion ended, so Backspace can still take it back after
//! the user typed on and deleted back to it (#782), and the bound that may forget it.

use super::words::is_separator;
use super::{CAPACITY, CommittedTail};

impl CommittedTail {
    /// A gõ tắt expansion just landed: the shadow ends right after it.
    pub fn mark_expansion_end(&mut self) {
        self.expansion_end = Some(self.tail.len());
    }

    /// The engine no longer remembers the expansion: there is nothing to come back to.
    pub fn forget_expansion_end(&mut self) {
        self.expansion_end = None;
    }

    /// Whether the caret is back exactly where the last expansion ended. The shadow is
    /// the text before the caret, so the same length is the same spot; the same text
    /// typed again further on is longer. Whether the expansion is still intact there
    /// is for [`CommittedTail::ends_with`] to say.
    pub fn at_expansion_end(&self) -> bool {
        self.expansion_end == Some(self.tail.len())
    }

    /// Keep the shadow bounded. Drops from the front only as far as a separator, so
    /// what remains never starts mid-word — and stays a suffix of the real text. The
    /// expansion end moves with the front, and is forgotten once the cut reaches it:
    /// the expansion is then no longer in the shadow to be checked.
    pub(super) fn trim(&mut self) {
        if self.tail.len() <= CAPACITY {
            return;
        }
        let floor = self.tail.len() - CAPACITY;
        let cut = self
            .tail
            .char_indices()
            .find(|(i, c)| *i >= floor && is_separator(*c))
            .map_or(self.tail.len(), |(i, c)| i + c.len_utf8());
        self.tail.drain(..cut);
        self.expansion_end = self
            .expansion_end
            .filter(|&end| end > cut)
            .map(|end| end - cut);
    }
}
