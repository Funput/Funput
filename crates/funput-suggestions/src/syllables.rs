//! The shipped Vietnamese syllables, ranked in four hand-written bands.
//!
//! Typo correction ranks its candidates with a prior of `ln(1 + uses) + 0.5`, where
//! `uses` is how often this user has typed the word. A fresh install has typed
//! nothing, so every candidate ties and the touch evidence decides alone. This list
//! gives the prior something to say before the user has: a syllable in band 1 is
//! worth [`SYLLABLE_TIER_WEIGHTS`]`[0]` pseudo-uses, band 4 one, an unlisted
//! syllable none. The engine's prior needs no change — a weight is simply added to
//! the user's own count.
//!
//! The list is `data/syllables/vi.txt` — hand-written, MIT, a ranking and never a
//! count (see `NOTICE.md` beside it). It is compiled in and parsed once, on first
//! use, into a table sorted by bytes; every lookup after that normalizes onto the
//! stack and binary-searches, with no allocation.
//!
//! This is **not** the veto. `SuggestionEngine::is_known_word` answers "is this a
//! word at all" for the deliberate English word or chat abbreviation that must not
//! be rewritten, and it does not consult this list: a Vietnamese syllable missing
//! from here only costs it some weight, never its right to exist.

use std::sync::OnceLock;

use crate::engine::MAX_TOKEN_SCALARS;
use crate::index::normalize;

const LIST: &str = include_str!("../data/syllables/vi.txt");

/// Pseudo-uses per band, band 1 first.
///
/// Through the engine's `ln(1 + uses) + 0.5` prior these come to about 2.1, 1.9,
/// 1.6 and 1.2 nats, against 0.5 for an unlisted syllable — so band 1 is worth
/// 1.61 nats over nothing, band 4 0.69. Deliberately light: 1.61 just clears the
/// engine's margin of 1.5, so the top band breaks an exact tie against an
/// unlisted word and does no more than that. The first draft was `[40, 12, 4, 1]`
/// (3.7 nats for band 1, about twice the margin), and `funput dev typos` over
/// Viet74K rejected it: the prior started overruling fingers that had clearly
/// leaned, and wrong repairs went up by half. The sweep is in
/// `docs/features/typo-correction.md` §12.
pub const SYLLABLE_TIER_WEIGHTS: [u32; 4] = [4, 3, 2, 1];

static TABLE: OnceLock<Box<[(&'static str, u8)]>> = OnceLock::new();

/// Every listed syllable with its band, sorted by bytes for the binary search.
///
/// The file is trusted to be lowercase NFC — `funput-lexicon-tool` checks it — so
/// no entry needs normalizing here and the table borrows straight from the source.
fn table() -> &'static [(&'static str, u8)] {
    TABLE.get_or_init(|| {
        let mut tier = 0u8;
        let mut entries = Vec::new();
        for line in LIST.lines().map(str::trim) {
            if let Some(band) = line.strip_prefix("## tier ") {
                tier = band.trim().parse().unwrap_or(0);
            } else if !line.is_empty() && !line.starts_with('#') && (1..=4).contains(&tier) {
                entries.push((line, tier));
            }
        }
        entries.sort_unstable_by(|left, right| left.0.as_bytes().cmp(right.0.as_bytes()));
        entries.dedup_by(|left, right| left.0 == right.0);
        entries.into_boxed_slice()
    })
}

/// The band `word` sits in — 1 for the most common — or `None` when it is not on
/// the list.
///
/// Case and normalization form do not matter: `Là`, `LÀ` and `la` + U+0300 all
/// answer 1. Allocation-free after the first call in the process.
pub fn syllable_tier(word: &str) -> Option<u8> {
    let mut buffer = [0u8; MAX_TOKEN_SCALARS * 4];
    let mut len = 0;
    for scalar in normalize::exact_chars(word) {
        if len + scalar.len_utf8() > buffer.len() {
            // Longer than any syllable on the list.
            return None;
        }
        len += scalar.encode_utf8(&mut buffer[len..]).len();
    }
    let key = &buffer[..len];
    let table = table();
    table
        .binary_search_by(|(entry, _)| entry.as_bytes().cmp(key))
        .ok()
        .map(|index| table[index].1)
}

/// The pseudo-uses `word`'s band is worth, and 0 for one not on the list — the
/// number a host adds to `SuggestionEngine::frequency` before it hands the sum to
/// `funput_engine_choose_correction`.
pub fn syllable_weight(word: &str) -> u32 {
    syllable_tier(word).map_or(0, |tier| SYLLABLE_TIER_WEIGHTS[usize::from(tier - 1)])
}

#[cfg(test)]
mod tests {
    use super::{SYLLABLE_TIER_WEIGHTS, syllable_tier, syllable_weight, table};

    #[test]
    fn the_most_common_words_sit_in_band_one_whatever_their_spelling() {
        assert_eq!(syllable_tier("là"), Some(1));
        assert_eq!(syllable_tier("Là"), Some(1));
        assert_eq!(syllable_tier("LÀ"), Some(1));
        assert_eq!(syllable_tier("la\u{0300}"), Some(1), "NFD input");
        assert_eq!(syllable_tier("không"), Some(1));
    }

    #[test]
    fn a_word_not_on_the_list_has_no_band_and_no_weight() {
        for word in ["dduwowfnh", "text", "", "l", "shopee"] {
            assert_eq!(syllable_tier(word), None, "{word}");
            assert_eq!(syllable_weight(word), 0, "{word}");
        }
        let long = "a".repeat(200);
        assert_eq!(syllable_tier(&long), None);
    }

    #[test]
    fn weights_follow_the_bands() {
        assert_eq!(syllable_weight("là"), SYLLABLE_TIER_WEIGHTS[0]);
        assert_eq!(
            syllable_tier("nguyễn"),
            Some(4),
            "a family name is real, but rare"
        );
        assert_eq!(syllable_weight("nguyễn"), SYLLABLE_TIER_WEIGHTS[3]);
        let mut seen = [false; 4];
        for (_, tier) in table() {
            seen[usize::from(tier - 1)] = true;
        }
        assert_eq!(seen, [true; 4], "every band has entries");
        assert!(
            SYLLABLE_TIER_WEIGHTS
                .windows(2)
                .all(|pair| pair[0] > pair[1]),
            "a more common band must weigh more"
        );
    }

    #[test]
    fn the_table_is_sorted_and_free_of_duplicates() {
        assert!(
            table()
                .windows(2)
                .all(|pair| pair[0].0.as_bytes() < pair[1].0.as_bytes())
        );
    }
}
