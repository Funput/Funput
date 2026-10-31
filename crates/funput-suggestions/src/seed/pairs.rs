//! The shipped pair list, parsed once into a table sorted by first syllable.
//!
//! `data/pairs/vi.txt` is written in the modern tone style. Building the table
//! adds the traditional spelling of every first syllable the styles disagree on,
//! so `hòa` and `hoà` both find their pair, and keeps both spellings of every
//! second syllable, so the bar writes the one the user chose. All of that happens
//! once, when a shell first turns the data on; a lookup after that normalizes onto
//! the stack and binary-searches, with no allocation.

use std::borrow::Cow;
use std::sync::OnceLock;

use funput_core::ToneStyle;

use super::respell::respell;
use crate::engine::MAX_TOKEN_SCALARS;
use crate::index::normalize;

const LIST: &str = include_str!("../../data/pairs/vi.txt");

static SHIPPED: OnceLock<PairTable> = OnceLock::new();

/// The table built from the shipped list, built on first use.
pub(crate) fn shipped() -> &'static PairTable {
    SHIPPED.get_or_init(|| PairTable::parse(LIST))
}

pub(crate) struct PairTable {
    entries: Box<[Entry]>,
}

struct Entry {
    first: Cow<'static, str>,
    modern: &'static str,
    traditional: Cow<'static, str>,
}

impl PairTable {
    /// Trusts `list` to be lowercase NFC with one second syllable per first one —
    /// `funput-lexicon-tool/tests/vi_pairs.rs` checks the shipped file. A first
    /// syllable listed twice keeps the line that came first.
    pub(crate) fn parse(list: &'static str) -> Self {
        let mut entries = Vec::new();
        for line in list.lines().map(str::trim) {
            if line.starts_with('#') {
                continue;
            }
            let Some((first, next)) = line.split_once(' ') else {
                continue;
            };
            let next = next.trim();
            let traditional: Cow<'static, str> =
                respell(next, ToneStyle::Traditional).map_or(Cow::Borrowed(next), Cow::Owned);
            if let Some(other) = respell(first, ToneStyle::Traditional) {
                entries.push(Entry {
                    first: Cow::Owned(other),
                    modern: next,
                    traditional: traditional.clone(),
                });
            }
            entries.push(Entry {
                first: Cow::Borrowed(first),
                modern: next,
                traditional,
            });
        }
        // Stable, so the earlier of two lines for one first syllable stays first.
        entries.sort_by(|left, right| left.first.as_bytes().cmp(right.first.as_bytes()));
        entries.dedup_by(|later, earlier| later.first == earlier.first);
        Self {
            entries: entries.into_boxed_slice(),
        }
    }

    /// The syllable that follows `previous`, spelled in `style`. Case and
    /// normalization form of `previous` do not matter.
    pub(crate) fn next_after(&self, previous: &str, style: ToneStyle) -> Option<&str> {
        let mut buffer = [0u8; MAX_TOKEN_SCALARS * 4];
        let key = normalize::exact_into(previous, &mut buffer)?;
        let index = self
            .entries
            .binary_search_by(|entry| entry.first.as_bytes().cmp(key.as_bytes()))
            .ok()?;
        let entry = &self.entries[index];
        Some(match style {
            ToneStyle::Modern => entry.modern,
            ToneStyle::Traditional => &entry.traditional,
        })
    }

    /// What the table holds on the heap. The table is shared by every engine in
    /// the process, so this is not part of any one engine's `stats`.
    #[cfg(test)]
    pub(crate) fn heap_bytes(&self) -> usize {
        let owned = |text: &Cow<'_, str>| match text {
            Cow::Owned(text) => text.capacity(),
            Cow::Borrowed(_) => 0,
        };
        self.entries.len() * size_of::<Entry>()
            + self
                .entries
                .iter()
                .map(|entry| owned(&entry.first) + owned(&entry.traditional))
                .sum::<usize>()
    }
}

#[cfg(test)]
mod tests {
    use funput_core::ToneStyle::{Modern, Traditional};

    use super::{PairTable, shipped};

    const LIST: &str = "# a comment\ncảm ơn\nhoà bình\nsức khoẻ\ncảm thấy\n\n";

    #[test]
    fn a_first_syllable_finds_its_pair_in_any_case_or_form() {
        let table = PairTable::parse(LIST);
        assert_eq!(table.next_after("cảm", Modern), Some("ơn"));
        assert_eq!(table.next_after("Cảm", Modern), Some("ơn"));
        assert_eq!(table.next_after("CA\u{0309}M", Modern), Some("ơn"), "NFD");
        assert_eq!(table.next_after("ơn", Modern), None);
        assert_eq!(table.next_after("", Modern), None);
        assert_eq!(table.next_after(&"a".repeat(200), Modern), None);
    }

    #[test]
    fn the_earlier_line_wins_a_repeated_first_syllable() {
        assert_eq!(PairTable::parse(LIST).next_after("cảm", Modern), Some("ơn"));
    }

    #[test]
    fn both_tone_styles_are_read_and_written() {
        let table = PairTable::parse(LIST);
        assert_eq!(table.next_after("hoà", Modern), Some("bình"));
        assert_eq!(table.next_after("hòa", Modern), Some("bình"));
        assert_eq!(table.next_after("sức", Modern), Some("khoẻ"));
        assert_eq!(table.next_after("sức", Traditional), Some("khỏe"));
        assert_eq!(table.next_after("cảm", Traditional), Some("ơn"));
    }

    #[test]
    fn the_shipped_table_stays_small() {
        let bytes = shipped().heap_bytes();
        assert!(
            bytes <= 128 * 1024,
            "{bytes} bytes is over the 128 KiB budget"
        );
    }
}
