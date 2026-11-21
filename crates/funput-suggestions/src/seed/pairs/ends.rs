//! Whole words whose last syllable also starts a shipped line.
//!
//! Predict reads the syllable before the caret, and that syllable is not always
//! the start of a word: `thông` after `giao` is the end of `giao thông`, and
//! offering `báo` there is a wrong guess built into the list. `data/pairs/vi-ends.txt`
//! names those words, so the shipped lines stay quiet after them.
//!
//! A word a line already spells (`phổ thông`, from the line `phổ thông`) needs no
//! entry here: [`super::ShippedPairs`] reads the lines as well.

use std::borrow::Cow;

use funput_core::{ToneStyle, respell_tone};

use crate::engine::MAX_TOKEN_SCALARS;
use crate::index::normalize;

/// Two-syllable words, sorted, in every tone spelling either syllable has.
pub(crate) struct WordEnds {
    words: Box<[Word]>,
}

type Word = (Cow<'static, str>, Cow<'static, str>);

impl WordEnds {
    /// Trusts `list` to be lowercase NFC, modern style, two syllables a line —
    /// `funput-lexicon-tool/tests/vi_pairs` checks the shipped file.
    pub(crate) fn parse(list: &'static str) -> Self {
        let mut words = Vec::new();
        let lines = list
            .lines()
            .filter(|line| !line.trim_start().starts_with('#'));
        for line in lines {
            let mut syllables = line.split_whitespace();
            let (Some(first), Some(last)) = (syllables.next(), syllables.next()) else {
                continue;
            };
            for first in spellings(first) {
                for last in spellings(last) {
                    words.push((first.clone(), last));
                }
            }
        }
        words.sort_unstable();
        words.dedup();
        Self {
            words: words.into_boxed_slice(),
        }
    }

    /// Whether `first last` is a listed word. `last` is already a lookup key
    /// (lowercase NFC); case and normalization form of `first` do not matter.
    pub(crate) fn holds(&self, first: &str, last: &str) -> bool {
        let mut buffer = [0u8; MAX_TOKEN_SCALARS * 4];
        let Some(first) = normalize::exact_into(first, &mut buffer) else {
            return false;
        };
        self.words
            .binary_search_by(|(left, right)| (&**left, &**right).cmp(&(first, last)))
            .is_ok()
    }

    /// What the list holds on the heap, as `PairTable::heap_bytes` counts it.
    #[cfg(test)]
    pub(super) fn heap_bytes(&self) -> usize {
        let owned = |text: &Cow<'_, str>| match text {
            Cow::Owned(text) => text.capacity(),
            Cow::Borrowed(_) => 0,
        };
        let texts = self
            .words
            .iter()
            .map(|(first, last)| owned(first) + owned(last));
        size_of_val(&*self.words) + texts.sum::<usize>()
    }
}

/// `syllable` as written, and its traditional spelling when the styles disagree.
fn spellings(syllable: &'static str) -> impl Iterator<Item = Cow<'static, str>> {
    let traditional = respell_tone(syllable, ToneStyle::Traditional).map(Cow::Owned);
    std::iter::once(Cow::Borrowed(syllable)).chain(traditional)
}

#[cfg(test)]
mod tests {
    use super::WordEnds;
    use crate::seed::pairs::shipped;

    const LIST: &str = "# a comment\ngiao thông\nhoà thuận\nalone\n\ngiao thông\n";

    #[test]
    fn a_listed_word_is_found_whatever_the_case_of_its_first_syllable() {
        let ends = WordEnds::parse(LIST);
        assert!(ends.holds("giao", "thông"));
        assert!(ends.holds("Giao", "thông"));
        assert!(!ends.holds("thông", "giao"));
        assert!(!ends.holds("giao", "thôn"));
        assert!(!ends.holds("", "thông"));
        assert!(!ends.holds("alone", ""));
    }

    #[test]
    fn both_tone_styles_of_either_syllable_are_found() {
        let ends = WordEnds::parse(LIST);
        assert!(ends.holds("hoà", "thuận"));
        assert!(ends.holds("hòa", "thuận"));
    }

    #[test]
    fn a_line_of_the_list_is_read_as_the_word_it_spells() {
        // `bưu điện` is a line, and `điện` starts one: after the whole word, the
        // lines say nothing; after `điện` alone, or after any other word, they do.
        let pairs = shipped();
        assert!(pairs.followers_in(&["bưu", "điện"]).is_empty());
        assert!(!pairs.followers_in(&["điện"]).is_empty());
        assert!(!pairs.followers_in(&["cái", "điện"]).is_empty());
        assert!(pairs.followers_in(&[]).is_empty());
    }

    #[test]
    fn a_listed_end_silences_the_line_its_last_syllable_starts() {
        let pairs = shipped();
        assert!(!pairs.followers_in(&["cảm"]).is_empty());
        assert!(pairs.followers_in(&["dũng", "cảm"]).is_empty());
        assert!(pairs.followers_in(&["Dũng", "Cảm"]).is_empty());
        assert!(!pairs.followers_in(&["dũng", "xin", "cảm"]).is_empty());
    }
}
