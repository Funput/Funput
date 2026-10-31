//! Finding where a key is, or where it may go, within its window.

use super::{NgramTable, PROBE_WINDOW};
use crate::ngram::key::start;
use crate::types::{WordRecord, WordRef};

/// What a key's window holds for it.
pub(super) enum Slot {
    /// The key's own entry.
    Hit(usize),
    /// An empty entry, or one whose context names a word that has since gone.
    Free(usize),
    /// Every entry is live and someone else's: the least seen of them.
    Full(usize),
}

impl<const K: usize> NgramTable<K> {
    /// Reads the whole window, so an entry emptied by a removal needs no
    /// tombstone: a key further along is still found.
    pub(super) fn probe(&self, key: &[WordRef; K], words: &[WordRecord]) -> Slot {
        let (mut free, mut dead, mut weakest) = (None, None, None::<usize>);
        for at in self.window(key) {
            let entry = &self.entries[at];
            if entry.key == *key {
                return Slot::Hit(at);
            }
            if entry.is_empty() {
                free.get_or_insert(at);
            } else if !entry.is_live(words) {
                dead.get_or_insert(at);
            } else if weakest.is_none_or(|w| entry.context_seen < self.entries[w].context_seen) {
                weakest = Some(at);
            }
        }
        match free.or(dead) {
            Some(at) => Slot::Free(at),
            None => Slot::Full(weakest.unwrap_or_default()),
        }
    }

    pub(super) fn window(&self, key: &[WordRef; K]) -> impl Iterator<Item = usize> + use<K> {
        let len = self.entries.len();
        let first = if len == 0 { 0 } else { start(key, len) };
        let mask = len.saturating_sub(1);
        (0..PROBE_WINDOW.min(len)).map(move |step| (first + step) & mask)
    }
}
