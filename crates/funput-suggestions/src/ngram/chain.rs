//! The words the journal wrote last, while each followed the one before.
//!
//! Replay rebuilds pairs from adjacency: a token marked chained followed the one
//! written before it. Two chained links in a row are therefore a trigram replay
//! can rebuild, and that is exactly when one is learned. The table on disk after
//! a crash then holds what the table in memory held, and no caller has to pass
//! two words to `learn_after` for it.

use super::MAX_CONTEXT;
use super::key::WordRef;

#[derive(Debug, Clone, Copy, Default)]
pub(crate) struct JournalChain {
    /// Oldest first; the most recent word is always the last element.
    words: [Option<WordRef>; MAX_CONTEXT],
}

impl JournalChain {
    /// Whatever is written next follows nothing the journal holds.
    pub(crate) fn clear(&mut self) {
        self.words = [None; MAX_CONTEXT];
    }

    pub(crate) fn last(&self) -> Option<WordRef> {
        self.words[MAX_CONTEXT - 1]
    }

    /// The word the last one followed, when it did.
    pub(crate) fn before_last(&self) -> Option<WordRef> {
        self.words[MAX_CONTEXT - 2]
    }

    /// `word` has just been written. Chained, it extends the run; otherwise it
    /// starts a new one.
    pub(crate) fn push(&mut self, word: WordRef, chained: bool) {
        if chained {
            self.words.rotate_left(1);
        } else {
            self.clear();
        }
        self.words[MAX_CONTEXT - 1] = Some(word);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn word(index: u32) -> WordRef {
        WordRef {
            index,
            generation: 0,
        }
    }

    #[test]
    fn a_chained_word_extends_the_run_and_an_unchained_one_restarts_it() {
        let mut chain = JournalChain::default();
        chain.push(word(1), false);
        assert_eq!((chain.before_last(), chain.last()), (None, Some(word(1))));
        chain.push(word(2), true);
        assert_eq!(
            (chain.before_last(), chain.last()),
            (Some(word(1)), Some(word(2)))
        );
        chain.push(word(3), false);
        assert_eq!((chain.before_last(), chain.last()), (None, Some(word(3))));
    }

    #[test]
    fn clearing_forgets_both_words() {
        let mut chain = JournalChain::default();
        chain.push(word(1), false);
        chain.push(word(2), true);
        chain.clear();
        assert_eq!((chain.before_last(), chain.last()), (None, None));
    }
}
