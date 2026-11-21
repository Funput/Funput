//! Where the shipped data fills the slots a prefix left empty.
//!
//! The user's own words keep the slots they took. After them, in order: the shipped
//! followers of the previous word that the prefix could become (`tuy` + `nh` →
//! `nhiên`), then the commonest listed syllables the prefix could become.
//! An unmarked prefix (`th`) may still be English, so the shipped words stop one slot
//! short, the English lexicon gets its turn, and only then is the rest filled. The
//! words are looked up once per keystroke and handed out over those two turns. See
//! `docs/features/vietnamese-seed-suggestion.md`, Phần B.

use super::SeedSlot;
use crate::engine::{MAX_TOKEN_SCALARS, SuggestionEngine};
use crate::index::{TOP_K, marks, normalize};
use crate::types::SuggestionSet;

/// The shipped words a prefix could become, best first, and how many are handed out.
pub(crate) struct SeedWords {
    words: [&'static str; TOP_K],
    len: usize,
    given: usize,
    /// An unmarked prefix, which might be English.
    leaves_one: bool,
}

impl SeedWords {
    const NONE: Self = Self {
        words: [""; TOP_K],
        len: 0,
        given: 0,
        leaves_one: false,
    };

    /// Fills `set` before the English lexicon: every slot for a marked prefix, which
    /// cannot be English, and all but the last for an unmarked one.
    pub(crate) fn fill_before_english(&mut self, set: &mut SuggestionSet<'_>) {
        self.fill(set, if self.leaves_one { TOP_K - 1 } else { TOP_K });
    }

    /// Fills whatever the English lexicon left empty.
    pub(crate) fn fill_rest(&mut self, set: &mut SuggestionSet<'_>) {
        self.fill(set, TOP_K);
    }

    fn fill(&mut self, set: &mut SuggestionSet<'_>, limit: usize) {
        while set.len < limit && self.given < self.len {
            let word = self.words[self.given];
            self.given += 1;
            if !contains(set, word) {
                set.push(word);
            }
        }
    }

    fn push(&mut self, word: &'static str) {
        if self.len < TOP_K {
            self.words[self.len] = word;
            self.len += 1;
        }
    }
}

impl SuggestionEngine {
    /// What the shipped data offers for `prefix` after `context`, leaving out what
    /// `taken` already holds and any word the user blocked. Nothing while it is off.
    pub(crate) fn seed_words(
        &self,
        context: &[&str],
        prefix: &str,
        taken: &SuggestionSet<'_>,
    ) -> SeedWords {
        let SeedSlot::Vietnamese {
            style,
            pairs,
            syllables,
        } = self.seed
        else {
            return SeedWords::NONE;
        };
        let (mut typed, mut folded) = ([0u8; MAX_TOKEN_SCALARS * 4], [0u8; MAX_TOKEN_SCALARS * 4]);
        let (Some(typed), Some(folded)) = (
            normalize::exact_into(prefix, &mut typed),
            normalize::folded_into(prefix, &mut folded),
        ) else {
            return SeedWords::NONE;
        };
        let mut seeds = SeedWords {
            leaves_one: typed == folded,
            ..SeedWords::NONE
        };
        for next in pairs.followers_in(context) {
            let word = next.spelled(style);
            if word != typed
                && marks::completes(typed, word)
                && !next
                    .spellings()
                    .any(|spelling| self.blocked.contains(spelling))
                && !contains(taken, word)
            {
                seeds.push(word);
            }
        }
        let paired = seeds.words;
        let (best, len) = syllables.top(typed, folded, style, |word| {
            paired.contains(&word) || self.blocked.contains(word) || contains(taken, word)
        });
        best[..len].iter().for_each(|word| seeds.push(word));
        seeds
    }
}

fn contains(set: &SuggestionSet<'_>, word: &str) -> bool {
    set.iter().any(|kept| kept == word)
}
