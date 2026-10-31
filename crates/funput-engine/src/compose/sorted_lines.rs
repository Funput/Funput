//! Membership in a sorted word list embedded as text, read in place.
//!
//! Shared by every English list the engine ships (doubled-key restore, collision
//! restore). The lists are data files pulled in with `include_str!`: one lowercase ASCII word
//! per line, in byte order, every line ending in `\n`. A `\r` before it is ignored,
//! so a list checked out with CRLF line endings still matches. A lookup is a binary
//! search over byte offsets that snaps each probe back to the start of its line, so
//! there is no index to build at startup and nothing to allocate per query.

use std::cmp::Ordering;

/// A sorted, newline-terminated list of lowercase ASCII words.
#[derive(Debug, Clone, Copy)]
pub(crate) struct SortedLines(&'static str);

impl SortedLines {
    pub(crate) const fn new(text: &'static str) -> Self {
        Self(text)
    }

    /// Whether `word` is listed, ignoring ASCII case. A word with anything outside
    /// ASCII cannot be listed, so it is refused before the search.
    pub(crate) fn contains(self, word: &str) -> bool {
        if word.is_empty() || !word.is_ascii() {
            return false;
        }
        let text = self.0.as_bytes();
        let (mut low, mut high) = (0, text.len());
        while low < high {
            let start = line_start(text, low + (high - low) / 2, low);
            let end = line_end(text, start);
            match compare(&text[start..end], word.as_bytes()) {
                Ordering::Equal => return true,
                Ordering::Less => low = end + 1,
                Ordering::Greater => high = start,
            }
        }
        false
    }

    /// Every listed word, in file order.
    #[cfg(test)]
    pub(crate) fn lines(self) -> std::str::Lines<'static> {
        self.0.lines()
    }

    /// The list exactly as embedded.
    #[cfg(test)]
    pub(crate) fn text(self) -> &'static str {
        self.0
    }
}

/// The start of the line holding byte `at`, never before `floor` (itself a line start).
fn line_start(text: &[u8], at: usize, floor: usize) -> usize {
    text[floor..at]
        .iter()
        .rposition(|&byte| byte == b'\n')
        .map_or(floor, |newline| floor + newline + 1)
}

/// The offset of the `\n` that ends the line starting at `start`.
fn line_end(text: &[u8], start: usize) -> usize {
    text[start..]
        .iter()
        .position(|&byte| byte == b'\n')
        .map_or(text.len(), |newline| start + newline)
}

/// Order a list line against the word being looked up, ignoring a trailing `\r`.
fn compare(line: &[u8], word: &[u8]) -> Ordering {
    let line = line.strip_suffix(b"\r").unwrap_or(line);
    line.iter()
        .copied()
        .cmp(word.iter().map(u8::to_ascii_lowercase))
}

/// What the binary search relies on: sorted, unique, lowercase ASCII words, each
/// line `\n`-terminated. Every shipped list asserts it.
#[cfg(test)]
pub(crate) fn assert_well_formed(list: SortedLines) {
    assert!(list.text().ends_with('\n'));
    let entries: Vec<&str> = list.lines().collect();
    assert!(!entries.is_empty());
    for entry in &entries {
        assert!(
            !entry.is_empty() && entry.bytes().all(|byte| byte.is_ascii_lowercase()),
            "{entry:?} is not a lowercase ASCII word"
        );
    }
    for pair in entries.windows(2) {
        assert!(
            pair[0] < pair[1],
            "{:?} is not before {:?}",
            pair[0],
            pair[1]
        );
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const FRUIT: SortedLines = SortedLines::new("apple\nbanana\ncherry\ndate\nfig\n");
    /// The same list as a Windows checkout without `.gitattributes` used to embed it.
    const FRUIT_CRLF: SortedLines =
        SortedLines::new("apple\r\nbanana\r\ncherry\r\ndate\r\nfig\r\n");

    #[test]
    fn finds_every_line_and_nothing_between() {
        for list in [FRUIT, FRUIT_CRLF] {
            for word in ["apple", "banana", "cherry", "date", "fig"] {
                assert!(list.contains(word), "{word}");
            }
            for word in ["", "a", "app", "apples", "blueberry", "egg", "zebra"] {
                assert!(!list.contains(word), "{word}");
            }
        }
    }

    #[test]
    fn ignores_ascii_case_and_refuses_non_ascii() {
        assert!(FRUIT.contains("Banana"));
        assert!(FRUIT.contains("FIG"));
        assert!(!FRUIT.contains("bánana"));
    }

    #[test]
    fn a_single_line_and_an_empty_list() {
        assert!(SortedLines::new("pass\n").contains("pass"));
        assert!(!SortedLines::new("pass\n").contains("past"));
        assert!(!SortedLines::new("").contains("pass"));
    }

    #[test]
    fn a_well_formed_list_passes() {
        assert_well_formed(FRUIT);
    }
}
