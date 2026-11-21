//! Building a table from a shipped list.

use std::borrow::Cow;

use funput_core::{ToneStyle, respell_tone};

use super::{Entry, Next, PairTable};
use crate::index::TOP_K;

impl PairTable {
    /// Trusts `list` to be lowercase NFC, modern style, one line per first word —
    /// `funput-lexicon-tool/tests/{vi,en}_pairs.rs` check the shipped files. A
    /// first word listed twice keeps the line that came first, and a line keeps
    /// the followers the bar has slots for.
    pub(crate) fn parse(list: &'static str) -> Self {
        let mut entries = Vec::new();
        let mut followers = Vec::new();
        let lines = list
            .lines()
            .filter(|line| !line.trim_start().starts_with('#'));
        for line in lines {
            let mut words = line.split_whitespace();
            let Some(first) = words.next() else {
                continue;
            };
            let start = followers.len();
            followers.extend(words.take(TOP_K).map(Next::new));
            let run = start..followers.len();
            if run.is_empty() {
                continue;
            }
            if let Some(other) = respell_tone(first, ToneStyle::Traditional) {
                entries.push(Entry {
                    first: Cow::Owned(other),
                    followers: run.clone(),
                });
            }
            entries.push(Entry {
                first: Cow::Borrowed(first),
                followers: run,
            });
        }
        // Stable, so the earlier of two lines for one first syllable stays first.
        entries.sort_by(|left, right| left.first.as_bytes().cmp(right.first.as_bytes()));
        entries.dedup_by(|later, earlier| later.first == earlier.first);
        Self {
            entries: entries.into_boxed_slice(),
            followers: followers.into_boxed_slice(),
        }
    }

    /// What the table holds on the heap. The table is shared by every engine in
    /// the process, so this is not part of any one engine's `stats`.
    #[cfg(test)]
    pub(super) fn heap_bytes(&self) -> usize {
        let firsts = self.entries.iter().map(|entry| match &entry.first {
            Cow::Owned(text) => text.capacity(),
            Cow::Borrowed(_) => 0,
        });
        size_of_val(&*self.entries)
            + size_of_val(&*self.followers)
            + firsts.sum::<usize>()
            + self.followers.iter().map(Next::heap_bytes).sum::<usize>()
    }
}
