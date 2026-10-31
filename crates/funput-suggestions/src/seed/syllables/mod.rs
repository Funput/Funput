//! The shipped syllable list, indexed for completing a syllable typed so far.
//!
//! The list (`data/syllables/vi.txt`, read through `crate::syllables::listed`) is in
//! rank order: band 1 first, roughly most common first within a band, so a line's
//! position is its rank. The index sorts it by the folded form (`đươ` and `dươ` both
//! as `duo`), so every syllable a prefix could become sits in one range found by
//! binary search; `marks::completes` then keeps the ones the typed marks allow.
//!
//! The list spells both ways the syllables the tone styles disagree on (`hoà` and
//! `hòa`). Each entry knows which style it belongs to, so the bar offers one spelling,
//! and where its other spelling sits, so a word blocked in either style stays out.

mod build;

use std::sync::OnceLock;

use funput_core::ToneStyle;

use super::marks;
use crate::index::{TOP_K, normalize};

static SHIPPED: OnceLock<SyllableIndex> = OnceLock::new();

/// The index of the shipped list, built on first use.
pub(crate) fn shipped() -> &'static SyllableIndex {
    SHIPPED.get_or_init(|| SyllableIndex::build(crate::syllables::listed().map(|(text, _)| text)))
}

const MODERN: u8 = 1;
const TRADITIONAL: u8 = 2;
const NO_OTHER: u16 = u16::MAX;

pub(crate) struct SyllableIndex {
    entries: Box<[Entry]>,
}

struct Entry {
    text: &'static str,
    /// Position in the list: lower is more common.
    rank: u16,
    /// The tone styles that spell it this way.
    styles: u8,
    /// Where the other style's spelling sits in `entries`, or `NO_OTHER`.
    other: u16,
}

impl SyllableIndex {
    /// The best `TOP_K` syllables the prefix could become in `style`, most common
    /// first, leaving out the one typed in full and any `skip` refuses. `typed` and
    /// `folded` are the prefix normalized once by the caller (`normalize::exact_into`,
    /// `normalize::folded_into`). `skip` sees both spellings of a syllable the styles
    /// disagree on, and only for a syllable good enough to be kept.
    pub(crate) fn top(
        &self,
        typed: &str,
        folded: &str,
        style: ToneStyle,
        mut skip: impl FnMut(&str) -> bool,
    ) -> ([&'static str; TOP_K], usize) {
        let wanted = match style {
            ToneStyle::Modern => MODERN,
            ToneStyle::Traditional => TRADITIONAL,
        };
        let start = self
            .entries
            .partition_point(|entry| normalize::folded_chars(entry.text).lt(folded.chars()));
        let mut best = [("", u16::MAX); TOP_K];
        let mut len = 0;
        for entry in self.entries[start..]
            .iter()
            .take_while(|entry| starts_with(entry.text, folded))
        {
            let worse_than_kept = len == TOP_K && entry.rank >= best[TOP_K - 1].1;
            if worse_than_kept
                || entry.styles & wanted == 0
                || entry.text == typed
                || !marks::completes(typed, entry.text)
                || skip(entry.text)
                || self.other_spelling(entry).is_some_and(&mut skip)
            {
                continue;
            }
            insert(&mut best, &mut len, (entry.text, entry.rank));
        }
        (best.map(|(text, _)| text), len)
    }

    fn other_spelling(&self, entry: &Entry) -> Option<&'static str> {
        self.entries
            .get(usize::from(entry.other))
            .map(|other| other.text)
    }
}

/// Whether `text` folded begins with `folded`: the range the index keeps.
fn starts_with(text: &str, folded: &str) -> bool {
    let mut letters = normalize::folded_chars(text);
    folded.chars().all(|typed| letters.next() == Some(typed))
}

/// Keeps `best[..len]` sorted by rank, at most `TOP_K` long.
fn insert(best: &mut [(&'static str, u16); TOP_K], len: &mut usize, item: (&'static str, u16)) {
    let at = best[..*len].partition_point(|kept| kept.1 <= item.1);
    if at >= TOP_K {
        return;
    }
    let end = (*len).min(TOP_K - 1);
    best.copy_within(at..end, at + 1);
    best[at] = item;
    *len = (*len + 1).min(TOP_K);
}

#[cfg(test)]
mod tests {
    use funput_core::ToneStyle::{Modern, Traditional};

    use super::{Entry, SyllableIndex, shipped};

    fn top(
        prefix: &str,
        style: funput_core::ToneStyle,
        skip: impl FnMut(&str) -> bool,
    ) -> Vec<&'static str> {
        let folded = crate::index::normalize::folded(prefix);
        let (best, len) = shipped().top(prefix, &folded, style, skip);
        best[..len].to_vec()
    }

    fn offered(prefix: &str, style: funput_core::ToneStyle) -> Vec<&'static str> {
        top(prefix, style, |_| false)
    }

    #[test]
    fn a_prefix_offers_the_most_common_syllables_it_could_become() {
        let offered = offered("đươ", Modern);
        assert_eq!(offered.first(), Some(&"được"), "{offered:?}");
        assert!(
            offered.iter().all(|word| word.starts_with("đư")),
            "{offered:?}"
        );
    }

    #[test]
    fn each_tone_style_gets_its_own_spelling_and_never_both() {
        let modern = offered("hoa", Modern);
        let traditional = offered("hoa", Traditional);
        assert!(
            modern.contains(&"hoà") && !modern.contains(&"hòa"),
            "{modern:?}"
        );
        assert!(
            traditional.contains(&"hòa") && !traditional.contains(&"hoà"),
            "{traditional:?}"
        );
    }

    #[test]
    fn a_skipped_spelling_skips_its_counterpart_too() {
        let kept = top("hoa", Modern, |word| word == "hòa");
        assert!(!kept.contains(&"hoà"), "{kept:?}");
    }

    #[test]
    fn the_syllable_typed_in_full_is_not_offered() {
        assert!(!offered("ăn", Modern).contains(&"ăn"));
    }

    #[test]
    fn the_index_stays_small() {
        let bytes = shipped().entries.len() * size_of::<Entry>();
        assert!(
            bytes <= 64 * 1024,
            "{bytes} bytes is over the 64 KiB budget"
        );
    }

    #[test]
    fn an_index_of_nothing_offers_nothing() {
        let empty = SyllableIndex::build(std::iter::empty());
        assert_eq!(empty.top("th", "th", Modern, |_| false).1, 0);
    }
}
