//! Following words around when one is removed from the store.
//!
//! The same rules `slots::relocate` applies to a word's followers, applied to
//! every entry's key as well. Removal is an edit the user made, not a keystroke,
//! so this may walk the whole table and set a few entries aside.

use super::{Entry, NgramTable};
use crate::bigram::follower::Follower;
use crate::bigram::slots;
use crate::ngram::key::WordRef;
use crate::types::WordRecord;

impl<const K: usize> NgramTable<K> {
    /// After `words.swap_remove`: entries whose context names the removed word or
    /// the vacated last slot go, entries naming the word that moved follow it,
    /// and every entry's followers are relocated as a word's are.
    ///
    /// A moved key hashes somewhere else, so those entries are lifted out and put
    /// back once the walk is done.
    pub(crate) fn relocate(
        &mut self,
        removed: Follower,
        vacated: u32,
        moved: Option<(Follower, Follower)>,
        words: &[WordRecord],
    ) {
        let mut lifted: Vec<Entry<K>> = Vec::new();
        for entry in self.entries.iter_mut().filter(|entry| !entry.is_empty()) {
            slots::relocate(
                &mut entry.followers,
                &mut entry.context_seen,
                removed,
                vacated,
                moved,
            );
            match rekey(&entry.key, removed, vacated, moved) {
                Rekey::Same => {}
                Rekey::Drop => *entry = Entry::EMPTY,
                Rekey::Moved(key) => {
                    lifted.push(Entry { key, ..*entry });
                    *entry = Entry::EMPTY;
                }
            }
        }
        for entry in lifted {
            self.insert(entry, words);
        }
    }
}

enum Rekey<const K: usize> {
    Same,
    Drop,
    Moved([WordRef; K]),
}

/// What removal does to one key, checked in the order `slots::relocate` checks an
/// edge: the moved word first, since it may have come from the vacated slot.
fn rekey<const K: usize>(
    key: &[WordRef; K],
    removed: Follower,
    vacated: u32,
    moved: Option<(Follower, Follower)>,
) -> Rekey<K> {
    let mut next = *key;
    let mut changed = false;
    for word in &mut next {
        if let Some((from, to)) = moved
            && word.is(from)
        {
            *word = WordRef {
                index: to.word,
                generation: to.generation,
            };
            changed = true;
        } else if word.is(removed) || word.index == vacated {
            return Rekey::Drop;
        }
    }
    if changed {
        Rekey::Moved(next)
    } else {
        Rekey::Same
    }
}
