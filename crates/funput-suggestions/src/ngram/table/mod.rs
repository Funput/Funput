//! A fixed-size table of contexts longer than one word.
//!
//! Open addressing over a boxed slice allocated once, the first time something is
//! written, and never grown: a full table makes room the way a word's follower
//! slots do, by giving up its weakest entry. Lookups and writes probe a bounded
//! window and allocate nothing, so the table costs the same on the millionth word
//! as on the first.
//!
//! - `entry` — one context and what followed it.
//! - `relocate` — keeping entries pointed at the right words when one is removed.

mod entry;
mod relocate;

pub(crate) use entry::Entry;

use super::key::{WordRef, hash};
use crate::bigram::slots;
use crate::types::WordRecord;

/// How many entries a key may land in. Every lookup reads the whole window, so a
/// removed entry needs no tombstone, and a write picks its victim from it.
pub(crate) const PROBE_WINDOW: usize = 8;

pub(crate) struct NgramTable<const K: usize> {
    entries: Box<[Entry<K>]>,
    /// A power of two, so a probe position is a mask away from the hash.
    capacity: usize,
}

impl<const K: usize> NgramTable<K> {
    /// A table that will hold `capacity` contexts once something is written.
    /// Allocates nothing until then.
    pub(crate) fn new(capacity: usize) -> Self {
        debug_assert!(capacity.is_power_of_two() && capacity >= PROBE_WINDOW);
        Self {
            entries: Box::default(),
            capacity,
        }
    }

    pub(crate) fn find(&self, key: &[WordRef; K]) -> Option<&Entry<K>> {
        self.window(key).find_map(|at| {
            let entry = &self.entries[at];
            (entry.key == *key).then_some(entry)
        })
    }

    /// Counts `next` as having followed `key`. Every word in `key` and `next` must
    /// be live; the caller has just checked.
    pub(crate) fn record(&mut self, key: [WordRef; K], next: WordRef, words: &[WordRecord]) {
        let at = self.slot_for(key, words);
        let entry = &mut self.entries[at];
        let dead = entry
            .followers
            .map(|slot| !slot.is_free() && slot.word(words).is_none());
        slots::record(
            &mut entry.followers,
            &mut entry.context_seen,
            dead,
            next.follower(),
        );
    }

    /// Puts back an entry read off disk or moved by a removal, giving up the
    /// weakest entry of its window if it has to.
    pub(crate) fn insert(&mut self, entry: Entry<K>, words: &[WordRecord]) {
        let at = self.slot_for(entry.key, words);
        self.entries[at] = entry;
    }

    /// The entries holding a context, live or not.
    pub(crate) fn entries(&self) -> impl Iterator<Item = &Entry<K>> {
        self.entries.iter().filter(|entry| !entry.is_empty())
    }

    /// Drops every entry and hands the memory back.
    pub(crate) fn clear(&mut self) {
        self.entries = Box::default();
    }

    pub(crate) fn heap_bytes(&self) -> usize {
        self.entries.len() * size_of::<Entry<K>>()
    }

    /// The entry `key` already has, or the one it should take: an empty one, then
    /// a dead one, then the least seen. Allocates the table on first use.
    fn slot_for(&mut self, key: [WordRef; K], words: &[WordRecord]) -> usize {
        if self.entries.is_empty() {
            self.entries = vec![Entry::EMPTY; self.capacity].into_boxed_slice();
        }
        let mut best = None;
        let mut best_rank = u32::MAX;
        for at in self.window(&key) {
            let entry = &self.entries[at];
            if entry.key == key {
                return at;
            }
            let rank = if entry.is_empty() {
                0
            } else if !entry.is_live(words) {
                1
            } else {
                2 + u32::from(entry.context_seen)
            };
            if rank < best_rank {
                (best, best_rank) = (Some(at), rank);
            }
        }
        let at = best.unwrap_or_default();
        self.entries[at] = Entry::new(key);
        at
    }

    fn window(&self, key: &[WordRef; K]) -> impl Iterator<Item = usize> + use<K> {
        let len = self.entries.len();
        let start = if len == 0 {
            0
        } else {
            hash(key) as usize & (len - 1)
        };
        (0..PROBE_WINDOW.min(len)).map(move |step| (start + step) & (len.max(1) - 1))
    }
}
