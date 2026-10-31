//! Finding the words of a context in the store.

use super::MAX_CONTEXT;
use super::key::WordRef;
use crate::engine::{MAX_TOKEN_SCALARS, SuggestionEngine};
use crate::index::normalize;

impl SuggestionEngine {
    /// The slot `previous` sits in, and the generation it sits there at.
    pub(crate) fn context_slot(&self, previous: &str) -> Option<WordRef> {
        self.context_slots(&[previous])[MAX_CONTEXT - 1]
    }

    /// The slots of the last `MAX_CONTEXT` words of `context`, oldest first and
    /// aligned to the end: the last word of `context` is always the last element.
    ///
    /// The last word first, and the one before it only when the last is in the
    /// store: a context with an unknown last word costs one scan, not two.
    /// Normalizes onto the stack and then compares with `==`, which is the same
    /// shape `upsert_word` uses: the length check reads out of the record itself
    /// and rejects almost every word without touching its heap buffer. This runs
    /// on every keystroke.
    ///
    /// Measured against one fused pass over both words: the same time when both
    /// are found, so the simpler shape stays.
    pub(crate) fn context_slots(&self, context: &[&str]) -> [Option<WordRef>; MAX_CONTEXT] {
        let mut found = [None; MAX_CONTEXT];
        let mut buffers = [[0u8; MAX_TOKEN_SCALARS * 4]; MAX_CONTEXT];
        let mut keys: [Option<&str>; MAX_CONTEXT] = [None; MAX_CONTEXT];
        let tail = &context[context.len().saturating_sub(MAX_CONTEXT)..];
        let offset = MAX_CONTEXT - tail.len();
        for ((key, buffer), text) in keys[offset..]
            .iter_mut()
            .zip(buffers[offset..].iter_mut())
            .zip(tail)
        {
            // `None` is a word longer than anything `learn` would have accepted.
            *key = normalize::exact_into(text, buffer);
        }
        let [older, Some(previous)] = keys else {
            // No last word, no context: the word before it has nothing to attach to.
            return found;
        };
        match older {
            None => found[1] = self.find_exact(previous),
            Some(older) => {
                found[1] = self.find_exact(previous);
                if found[1].is_some() {
                    found[0] = self.find_exact(older);
                }
            }
        }
        found
    }

    /// One word: the tight scan `context_slot` always was.
    fn find_exact(&self, key: &str) -> Option<WordRef> {
        let index = self.words.iter().position(|word| word.text == key)?;
        WordRef::at(index as u32, &self.words)
    }
}

// The shape above reads two words. A 4-gram teaches it a third.
const _: () = assert!(MAX_CONTEXT == 2);
