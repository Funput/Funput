//! The shipped pair lists, each parsed once into its own table sorted by first word.
//!
//! Two lists: `data/pairs/vi.txt`, the point of the feature, and `data/pairs/en.txt`,
//! a short English supplement for people who type English in Vietnamese mode. They
//! stay separate tables ([`ShippedPairs`]) so a mode can ask only the lists it allows
//! (English mode, a listed extension, would ask only the English one). Their tests
//! keep them apart: no English first word is a Vietnamese syllable, and no first word
//! is listed in both. The English words are ASCII, so everything below about tone
//! styles leaves them as they are.
//!
//! A line is a first word and the words that follow it, best first: as many as the
//! bar has slots. `data/pairs/vi.txt` is written in the modern tone style. Building
//! the table adds the traditional spelling of every first syllable the styles
//! disagree on, so `hòa` and `hoà` both find their line, and keeps both spellings of
//! every following syllable, so the bar writes the one the user chose. Where the tone
//! goes is `funput_core::respell_tone`'s call, the same rule the composer types by.
//! All of that happens once, when a shell first turns the data on; a lookup after
//! that normalizes onto the stack and binary-searches, with no allocation.
//!
//! - `next` — a following word, in both tone styles.
//! - `parse` — building a table from a list.
//! - `ends` — the words a first syllable ends, after which the lines stay quiet.

mod ends;
mod next;
mod parse;

use std::borrow::Cow;
use std::ops::Range;
use std::sync::OnceLock;

use ends::WordEnds;
pub(crate) use next::Next;

use crate::engine::MAX_TOKEN_SCALARS;
use crate::index::normalize;

const VIETNAMESE: &str = include_str!("../../../data/pairs/vi.txt");
const VIETNAMESE_ENDS: &str = include_str!("../../../data/pairs/vi-ends.txt");
const ENGLISH: &str = include_str!("../../../data/pairs/en.txt");

static SHIPPED: OnceLock<ShippedPairs> = OnceLock::new();

/// The tables built from the shipped lists, built on first use.
pub(crate) fn shipped() -> &'static ShippedPairs {
    SHIPPED.get_or_init(|| ShippedPairs {
        vietnamese: PairTable::parse(VIETNAMESE),
        ends: WordEnds::parse(VIETNAMESE_ENDS),
        english: PairTable::parse(ENGLISH),
    })
}

/// Every shipped list, one table each.
pub(crate) struct ShippedPairs {
    vietnamese: PairTable,
    ends: WordEnds,
    english: PairTable,
}

impl ShippedPairs {
    /// What Vietnamese mode offers after the last word of `context`, best first:
    /// the Vietnamese line, else the English supplement's (a first word both lead
    /// is refused by the tests, and Vietnamese would win it).
    ///
    /// Nothing when the word before it shows that last word to be the end of a
    /// word and not the start of one: `thông` after `giao` starts nothing.
    pub(crate) fn followers_in(&self, context: &[&str]) -> &[Next] {
        let Some((previous, before)) = context.split_last() else {
            return &[];
        };
        match before.last() {
            Some(older) if self.ends_word(older, previous) => &[],
            _ => self.followers_after(previous),
        }
    }

    fn followers_after(&self, previous: &str) -> &[Next] {
        match self.vietnamese.followers_after(previous) {
            [] => self.english.followers_after(previous),
            followers => followers,
        }
    }

    /// Whether `older previous` is one word: a listed end, or a line of the
    /// Vietnamese list read as the word it spells.
    fn ends_word(&self, older: &str, previous: &str) -> bool {
        let mut buffer = [0u8; MAX_TOKEN_SCALARS * 4];
        let Some(last) = normalize::exact_into(previous, &mut buffer) else {
            return false;
        };
        let spelled = |next: &Next| next.spellings().any(|spelling| spelling == last);
        self.ends.holds(older, last) || self.vietnamese.followers_after(older).iter().any(spelled)
    }
}

/// One list: its first words, sorted, each naming its run of `followers`.
pub(crate) struct PairTable {
    entries: Box<[Entry]>,
    followers: Box<[Next]>,
}

struct Entry {
    first: Cow<'static, str>,
    /// Where this first word's followers sit in [`PairTable::followers`]. Both
    /// tone spellings of a first word name the same run.
    followers: Range<usize>,
}

impl PairTable {
    /// The words that follow `previous`, best first; none for a word no line
    /// starts with. Case, normalization form and tone style of `previous` do not
    /// matter.
    pub(crate) fn followers_after(&self, previous: &str) -> &[Next] {
        let mut buffer = [0u8; MAX_TOKEN_SCALARS * 4];
        let Some(key) = normalize::exact_into(previous, &mut buffer) else {
            return &[];
        };
        match self
            .entries
            .binary_search_by(|entry| entry.first.as_bytes().cmp(key.as_bytes()))
        {
            Ok(index) => &self.followers[self.entries[index].followers.clone()],
            Err(_) => &[],
        }
    }
}

#[cfg(test)]
mod tests {
    use funput_core::ToneStyle::{self, Modern, Traditional};

    use super::{PairTable, shipped};

    const LIST: &str =
        "# a comment\ncảm ơn\nhoà bình\nsức khoẻ mạnh\ncảm thấy\nxin chào lỗi phép mời\n\n";

    fn after<'a>(table: &'a PairTable, previous: &str, style: ToneStyle) -> Vec<&'a str> {
        let followers = table.followers_after(previous).iter();
        followers.map(|next| next.spelled(style)).collect()
    }

    #[test]
    fn a_first_syllable_finds_its_line_in_any_case_or_form() {
        let table = PairTable::parse(LIST);
        assert_eq!(after(&table, "cảm", Modern), ["ơn"]);
        assert_eq!(after(&table, "Cảm", Modern), ["ơn"]);
        assert_eq!(after(&table, "CA\u{0309}M", Modern), ["ơn"]);
        assert!(after(&table, "ơn", Modern).is_empty());
        assert!(after(&table, "", Modern).is_empty());
        assert!(after(&table, &"a".repeat(200), Modern).is_empty());
    }

    #[test]
    fn a_line_lists_its_followers_best_first_up_to_the_bar() {
        let table = PairTable::parse(LIST);
        assert_eq!(after(&table, "xin", Modern), ["chào", "lỗi", "phép"]);
        assert_eq!(after(&table, "sức", Traditional), ["khỏe", "mạnh"]);
    }

    #[test]
    fn the_earlier_line_wins_a_repeated_first_syllable() {
        let table = PairTable::parse(LIST);
        assert_eq!(after(&table, "cảm", Modern), ["ơn"]);
    }

    #[test]
    fn vietnamese_mode_asks_the_vietnamese_list_then_the_english_one() {
        let pairs = shipped();
        let first = |previous| {
            let followers = pairs.followers_after(previous);
            followers.first().map(|next| next.spelled(Modern))
        };
        assert_eq!(first("cảm"), Some("ơn"));
        assert_eq!(first("thank"), Some("you"));
        assert_eq!(first("nothing"), None);
    }

    #[test]
    fn both_tone_styles_are_read_and_written() {
        let table = PairTable::parse(LIST);
        assert_eq!(after(&table, "hoà", Modern), ["bình"]);
        assert_eq!(after(&table, "hòa", Modern), ["bình"]);
        assert_eq!(after(&table, "sức", Modern), ["khoẻ", "mạnh"]);
        assert_eq!(after(&table, "cảm", Traditional), ["ơn"]);
        let spellings: Vec<&str> = table.followers_after("sức")[0].spellings().collect();
        assert_eq!(spellings, ["khoẻ", "khỏe"]);
    }

    #[test]
    fn the_shipped_tables_stay_small() {
        let pairs = shipped();
        let tables = pairs.vietnamese.heap_bytes() + pairs.english.heap_bytes();
        let bytes = tables + pairs.ends.heap_bytes();
        assert!(
            bytes <= 128 * 1024,
            "{bytes} bytes is over the 128 KiB budget"
        );
    }
}
