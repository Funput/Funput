//! A fixed-size table of contexts longer than one word.
//!
//! Open addressing over a boxed slice allocated once, the first time something is
//! written, and never grown. Lookups and writes probe a bounded window and
//! allocate nothing, so the table costs the same on the millionth word as on the
//! first.
//!
//! A full window makes room the way a word's follower slots do: a newcomer
//! challenges the least seen entry, and takes its place only once as many
//! newcomers have asked as that entry has been seen. A phrase typed once never
//! pushes out a habit; one typed again and again does.
//!
//! - `entry` — one context and what followed it.
//! - `probe` — where a key is, or may go, within its window.
//! - `relocate` — keeping entries pointed at the right words when one is removed.

mod entry;
mod probe;
mod relocate;

pub(crate) use entry::Entry;

use probe::Slot;

use crate::bigram::follower::Follower;
use crate::bigram::slots;
use crate::types::{WordRecord, WordRef};

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
    /// be live; the caller has just checked. A newcomer that loses its challenge
    /// is not recorded at all.
    pub(crate) fn record(&mut self, key: [WordRef; K], next: WordRef, words: &[WordRecord]) {
        self.allocate();
        let at = match self.probe(&key, words) {
            Slot::Hit(at) => at,
            Slot::Free(at) => self.take(at, key),
            Slot::Full(weakest) => {
                let entry = &mut self.entries[weakest];
                entry.challenges = entry.challenges.saturating_add(1);
                if entry.challenges < entry.context_seen {
                    return;
                }
                self.take(weakest, key)
            }
        };
        let entry = &mut self.entries[at];
        // Seen again: whoever was challenging it starts over.
        entry.challenges = 0;
        let dead = entry
            .followers
            .map(|slot| !slot.is_free() && slot.word(words).is_none());
        slots::record(
            &mut entry.followers,
            &mut entry.context_seen,
            dead,
            Follower::to(next),
        );
    }

    /// Puts back an entry read off disk or moved by a removal. In a full window it
    /// only displaces an entry seen less often than itself.
    pub(crate) fn insert(&mut self, entry: Entry<K>, words: &[WordRecord]) {
        self.allocate();
        match self.probe(&entry.key, words) {
            Slot::Hit(at) | Slot::Free(at) => self.entries[at] = entry,
            Slot::Full(weakest) if self.entries[weakest].context_seen < entry.context_seen => {
                self.entries[weakest] = entry;
            }
            Slot::Full(_) => {}
        }
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

    fn allocate(&mut self) {
        if self.entries.is_empty() {
            self.entries = vec![Entry::EMPTY; self.capacity].into_boxed_slice();
        }
    }

    fn take(&mut self, at: usize, key: [WordRef; K]) -> usize {
        self.entries[at] = Entry::new(key);
        at
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// One window: with eight entries every key probes the whole table, so these
    /// tests decide exactly who competes with whom.
    fn table() -> NgramTable<2> {
        NgramTable::new(PROBE_WINDOW)
    }

    fn words() -> Vec<WordRecord> {
        (0..24)
            .map(|n| WordRecord::new(format!("w{n}"), 1, 0))
            .collect()
    }

    fn key(n: u32, words: &[WordRecord]) -> [WordRef; 2] {
        [
            WordRef::at(n, words).unwrap(),
            WordRef::at(n + 1, words).unwrap(),
        ]
    }

    fn seen(table: &NgramTable<2>, n: u32, words: &[WordRecord]) -> Option<u16> {
        table.find(&key(n, words)).map(|entry| entry.context_seen)
    }

    /// Eight contexts at `n * 2`, each seen `times` times, filling the window.
    fn full(table: &mut NgramTable<2>, words: &[WordRecord], times: usize) {
        let next = WordRef::at(23, words).unwrap();
        for n in 0..8 {
            for _ in 0..times {
                table.record(key(n * 2, words), next, words);
            }
        }
    }

    #[test]
    fn nothing_is_allocated_until_the_first_record_and_clear_hands_it_back() {
        let words = words();
        let mut table = table();
        assert_eq!(table.heap_bytes(), 0);
        assert!(table.find(&key(0, &words)).is_none());
        full(&mut table, &words, 1);
        assert_eq!(table.heap_bytes(), PROBE_WINDOW * size_of::<Entry<2>>());
        table.clear();
        assert_eq!(table.heap_bytes(), 0);
    }

    #[test]
    fn a_newcomer_takes_a_place_only_after_as_many_challenges_as_it_was_seen() {
        let words = words();
        let mut table = table();
        full(&mut table, &words, 2);
        let next = WordRef::at(23, &words).unwrap();
        table.record(key(20, &words), next, &words);
        assert_eq!(
            seen(&table, 20, &words),
            None,
            "one sighting never pushes out a habit"
        );
        assert_eq!(table.entries().count(), 8);

        table.record(key(20, &words), next, &words);
        assert_eq!(
            seen(&table, 20, &words),
            Some(1),
            "persistence wins the place"
        );
        assert_eq!(table.entries().count(), 8);
    }

    #[test]
    fn being_seen_again_starts_the_challenge_over() {
        let words = words();
        let mut table = table();
        full(&mut table, &words, 2);
        let next = WordRef::at(23, &words).unwrap();
        table.record(key(20, &words), next, &words);
        // The challenged entry is the first of the window's least seen ones.
        let challenged = (0..8).find(|n| {
            table
                .find(&key(n * 2, &words))
                .is_some_and(|entry| entry.challenges == 1)
        });
        let challenged = challenged.unwrap() * 2;
        table.record(key(challenged, &words), next, &words);
        table.record(key(20, &words), next, &words);
        assert_eq!(seen(&table, 20, &words), None);
        assert_eq!(seen(&table, challenged, &words), Some(3));
    }

    #[test]
    fn a_dead_entry_is_given_up_before_any_live_one() {
        let mut words = words();
        let mut table = table();
        full(&mut table, &words, 5);
        words[6].generation += 1;
        let next = WordRef::at(23, &words).unwrap();
        table.record(key(20, &words), next, &words);
        assert_eq!(
            seen(&table, 20, &words),
            Some(1),
            "no challenge while a dead entry is free"
        );
        for n in [0, 2, 4, 8, 10, 12, 14] {
            assert_eq!(seen(&table, n, &words), Some(5), "{n} must survive");
        }
    }

    #[test]
    fn an_emptied_entry_does_not_hide_the_keys_after_it() {
        let words = words();
        let mut table = table();
        full(&mut table, &words, 1);
        let first = table.window(&key(0, &words)).next().unwrap();
        table.entries[first] = Entry::EMPTY;
        assert_eq!(table.entries().count(), 7);
        let found = (0..8)
            .filter(|n| table.find(&key(n * 2, &words)).is_some())
            .count();
        assert_eq!(found, 7);
    }

    #[test]
    fn insert_only_displaces_an_entry_seen_less_often() {
        let words = words();
        let mut table = table();
        full(&mut table, &words, 2);
        let weak = Entry {
            context_seen: 1,
            ..Entry::new(key(20, &words))
        };
        table.insert(weak, &words);
        assert_eq!(seen(&table, 20, &words), None);
        let strong = Entry {
            context_seen: 3,
            ..Entry::new(key(20, &words))
        };
        table.insert(strong, &words);
        assert_eq!(seen(&table, 20, &words), Some(3));
        assert_eq!(table.entries().count(), 8);
    }
}
