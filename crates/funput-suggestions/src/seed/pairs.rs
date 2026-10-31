//! The shipped pair lists, parsed once into one table sorted by first word.
//!
//! Two lists share it: `data/pairs/vi.txt`, the point of the feature, and
//! `data/pairs/en.txt`, a short English list for people who type English in
//! Vietnamese mode. Their tests keep the two apart: no English first word is a
//! Vietnamese syllable, and no first word is listed in both. The English words are
//! ASCII, so everything below about tone styles leaves them as they are.
//!
//! `data/pairs/vi.txt` is written in the modern tone style. Building the table
//! adds the traditional spelling of every first syllable the styles disagree on,
//! so `hòa` and `hoà` both find their pair, and keeps both spellings of every
//! second syllable, so the bar writes the one the user chose. Where the tone goes
//! is `funput_core::respell_tone`'s call, the same rule the composer types by.
//! All of that happens once, when a shell first turns the data on; a lookup after
//! that normalizes onto the stack and binary-searches, with no allocation.

use std::borrow::Cow;
use std::sync::OnceLock;

use funput_core::{ToneStyle, respell_tone};

use crate::engine::MAX_TOKEN_SCALARS;
use crate::index::normalize;

/// Vietnamese first, so a first word listed in both (which the tests forbid) would
/// keep its Vietnamese pair.
const LISTS: [&str; 2] = [
    include_str!("../../data/pairs/vi.txt"),
    include_str!("../../data/pairs/en.txt"),
];

static SHIPPED: OnceLock<PairTable> = OnceLock::new();

/// The table built from the shipped lists, built on first use.
pub(crate) fn shipped() -> &'static PairTable {
    SHIPPED.get_or_init(|| PairTable::parse(LISTS))
}

pub(crate) struct PairTable {
    entries: Box<[Entry]>,
}

struct Entry {
    first: Cow<'static, str>,
    next: Next,
}

/// The second syllable of a pair, in both tone styles.
#[derive(Clone)]
pub(crate) struct Next {
    modern: &'static str,
    traditional: Cow<'static, str>,
}

impl Next {
    fn new(modern: &'static str) -> Self {
        let traditional =
            respell_tone(modern, ToneStyle::Traditional).map_or(Cow::Borrowed(modern), Cow::Owned);
        Self {
            modern,
            traditional,
        }
    }

    /// The spelling the bar writes for a user who places tones in `style`.
    pub(crate) fn spelled(&self, style: ToneStyle) -> &str {
        match style {
            ToneStyle::Modern => self.modern,
            ToneStyle::Traditional => &self.traditional,
        }
    }

    /// Both spellings, the same one twice when the styles agree. A word the user
    /// blocked in one style stays blocked in the other.
    pub(crate) fn spellings(&self) -> impl Iterator<Item = &str> {
        [self.modern, &*self.traditional].into_iter()
    }
}

impl PairTable {
    /// Trusts each list to be lowercase NFC, modern style, with one second word per
    /// first one — `funput-lexicon-tool/tests/{vi,en}_pairs.rs` check the shipped
    /// files. A first word listed twice keeps the line that came first, earlier lists
    /// before later ones.
    pub(crate) fn parse(lists: impl IntoIterator<Item = &'static str>) -> Self {
        let mut entries = Vec::new();
        for line in lists.into_iter().flat_map(str::lines).map(str::trim) {
            if line.starts_with('#') {
                continue;
            }
            let Some((first, next)) = line.split_once(' ') else {
                continue;
            };
            let next = Next::new(next.trim());
            if let Some(other) = respell_tone(first, ToneStyle::Traditional) {
                entries.push(Entry {
                    first: Cow::Owned(other),
                    next: next.clone(),
                });
            }
            entries.push(Entry {
                first: Cow::Borrowed(first),
                next,
            });
        }
        // Stable, so the earlier of two lines for one first syllable stays first.
        entries.sort_by(|left, right| left.first.as_bytes().cmp(right.first.as_bytes()));
        entries.dedup_by(|later, earlier| later.first == earlier.first);
        Self {
            entries: entries.into_boxed_slice(),
        }
    }

    /// The syllable that follows `previous`. Case, normalization form and tone
    /// style of `previous` do not matter.
    pub(crate) fn next_after(&self, previous: &str) -> Option<&Next> {
        let mut buffer = [0u8; MAX_TOKEN_SCALARS * 4];
        let key = normalize::exact_into(previous, &mut buffer)?;
        let index = self
            .entries
            .binary_search_by(|entry| entry.first.as_bytes().cmp(key.as_bytes()))
            .ok()?;
        Some(&self.entries[index].next)
    }
}

#[cfg(test)]
mod tests {
    use std::borrow::Cow;

    use funput_core::ToneStyle::{self, Modern, Traditional};

    use super::{Entry, PairTable, shipped};

    const LIST: &str = "# a comment\ncảm ơn\nhoà bình\nsức khoẻ\ncảm thấy\n\n";

    fn after(table: &PairTable, previous: &str, style: ToneStyle) -> Option<String> {
        table
            .next_after(previous)
            .map(|next| next.spelled(style).to_owned())
    }

    /// What the table holds on the heap. The table is shared by every engine in
    /// the process, so this is not part of any one engine's `stats`.
    fn heap_bytes(table: &PairTable) -> usize {
        let owned = |text: &Cow<'_, str>| match text {
            Cow::Owned(text) => text.capacity(),
            Cow::Borrowed(_) => 0,
        };
        table.entries.len() * size_of::<Entry>()
            + table
                .entries
                .iter()
                .map(|entry| owned(&entry.first) + owned(&entry.next.traditional))
                .sum::<usize>()
    }

    #[test]
    fn a_first_syllable_finds_its_pair_in_any_case_or_form() {
        let table = PairTable::parse([LIST]);
        assert_eq!(after(&table, "cảm", Modern).as_deref(), Some("ơn"));
        assert_eq!(after(&table, "Cảm", Modern).as_deref(), Some("ơn"));
        assert_eq!(after(&table, "CA\u{0309}M", Modern).as_deref(), Some("ơn"));
        assert_eq!(after(&table, "ơn", Modern), None);
        assert_eq!(after(&table, "", Modern), None);
        assert_eq!(after(&table, &"a".repeat(200), Modern), None);
    }

    #[test]
    fn the_earlier_line_wins_a_repeated_first_syllable() {
        let table = PairTable::parse([LIST]);
        assert_eq!(after(&table, "cảm", Modern).as_deref(), Some("ơn"));
    }

    #[test]
    fn an_earlier_list_wins_a_first_word_both_list() {
        let table = PairTable::parse(["thank you\n", "thank god\nsuch as\n"]);
        assert_eq!(after(&table, "thank", Modern).as_deref(), Some("you"));
        assert_eq!(after(&table, "such", Modern).as_deref(), Some("as"));
    }

    #[test]
    fn both_tone_styles_are_read_and_written() {
        let table = PairTable::parse([LIST]);
        assert_eq!(after(&table, "hoà", Modern).as_deref(), Some("bình"));
        assert_eq!(after(&table, "hòa", Modern).as_deref(), Some("bình"));
        assert_eq!(after(&table, "sức", Modern).as_deref(), Some("khoẻ"));
        assert_eq!(after(&table, "sức", Traditional).as_deref(), Some("khỏe"));
        assert_eq!(after(&table, "cảm", Traditional).as_deref(), Some("ơn"));
        let spellings: Vec<&str> = table.next_after("sức").unwrap().spellings().collect();
        assert_eq!(spellings, ["khoẻ", "khỏe"]);
    }

    #[test]
    fn the_shipped_table_stays_small() {
        let bytes = heap_bytes(shipped());
        assert!(
            bytes <= 128 * 1024,
            "{bytes} bytes is over the 128 KiB budget"
        );
    }
}
