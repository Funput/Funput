//! The dictionary as a list: what a settings page shows, filtered, searched and
//! sorted. Off the typing path entirely, so it allocates freely.

use super::Origin;
use crate::engine::SuggestionEngine;
use crate::index::normalize;
use crate::types::WordRecord;

/// Which words a listing includes.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum DictionaryFilter {
    #[default]
    All,
    Learned,
    Added,
}

/// The order a listing comes back in.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum DictionarySort {
    /// Most used first, then most recent.
    #[default]
    MostUsed,
    /// Most recently used or added first.
    Recent,
    /// Alphabetical with Vietnamese marks ignored first, so `ăn` sorts beside `an`
    /// rather than after `z`.
    Alphabetical,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct DictionaryQuery<'a> {
    pub filter: DictionaryFilter,
    pub sort: DictionarySort,
    /// Matched anywhere in the word, marks and case ignored: `duoc` finds `được`.
    /// Empty matches everything.
    pub search: &'a str,
}

/// One word as the user sees it.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct DictionaryEntry<'a> {
    pub word: &'a str,
    pub uses: u32,
    /// The engine's sequence when the word was last typed or added: comparable
    /// between entries, not a time.
    pub last_used: u64,
    pub origin: Origin,
}

impl SuggestionEngine {
    /// The words the bar can offer. Words still below the promotion threshold are left out: most of them are slips, and a
    /// list of every slip is not a dictionary.
    pub fn dictionary(&self, query: &DictionaryQuery<'_>) -> Vec<DictionaryEntry<'_>> {
        let needle = normalize::folded(query.search);
        let mut entries: Vec<_> = self
            .words
            .iter()
            .filter(|word| self.offers(word) && admits(query.filter, word.origin))
            .filter(|word| needle.is_empty() || normalize::folded(&word.text).contains(&needle))
            .map(|word| self.entry(word))
            .collect();
        match query.sort {
            DictionarySort::MostUsed => entries.sort_by(|left, right| {
                (right.uses, right.last_used, left.word).cmp(&(
                    left.uses,
                    left.last_used,
                    right.word,
                ))
            }),
            DictionarySort::Recent => entries.sort_by(|left, right| {
                (right.last_used, left.word).cmp(&(left.last_used, right.word))
            }),
            DictionarySort::Alphabetical => {
                entries.sort_by_cached_key(|entry| (normalize::folded(entry.word), entry.word));
            }
        }
        entries
    }

    /// The blocked words, in code point order.
    pub fn blocked_words(&self) -> impl ExactSizeIterator<Item = &str> + '_ {
        self.exclusions
            .blocked
            .as_slice()
            .iter()
            .map(String::as_str)
    }

    fn entry<'a>(&self, word: &'a WordRecord) -> DictionaryEntry<'a> {
        DictionaryEntry {
            word: &word.text,
            uses: word.uses,
            last_used: word.last_used,
            origin: word.origin,
        }
    }
}

fn admits(filter: DictionaryFilter, origin: Origin) -> bool {
    match filter {
        DictionaryFilter::All => true,
        DictionaryFilter::Learned => origin == Origin::Learned,
        DictionaryFilter::Added => origin == Origin::Added,
    }
}
