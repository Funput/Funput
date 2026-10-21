//! Membership in a sorted word list embedded as text, read in place.
//!
//! The lists are data files pulled in with `include_str!`: one lowercase ASCII word
//! per line, in byte order, every line ending in `\n`. A lookup is a binary search
//! over byte offsets that snaps each probe back to the start of its line, so there
//! is no index to build at startup and nothing to allocate per query.

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

fn compare(line: &[u8], word: &[u8]) -> Ordering {
    line.iter()
        .copied()
        .cmp(word.iter().map(u8::to_ascii_lowercase))
}
