//! Split a syllable chunk into onset + vowel nucleus + coda.
//!
//! Runs on the keystroke hot path, so parsing borrows the buffer and never
//! allocates: the onset is a prefix slice; nucleus and coda are filtering
//! iterators over the remainder.

mod onset;

use crate::unicode::marks::is_vowel;

pub(crate) use onset::is_valid_onset;

/// Parsed view of a single syllable chunk. The nucleus and coda are
/// *interleaved* selections of the post-onset text — a vowel after a consonant
/// still counts as nucleus (`mixa` → nucleus `ia`, coda `x`) — so they are
/// exposed as iterators rather than slices. That order-blind view is the lenient
/// one; a strict verdict must also check [`Self::is_well_ordered`].
#[derive(Debug, Clone, Copy)]
pub struct SyllableParts<'a> {
    pub onset: &'a str,
    rest: &'a str,
    /// Leading consonants do not form a valid Vietnamese onset.
    pub invalid_onset: bool,
}

impl<'a> SyllableParts<'a> {
    /// The vowel nucleus, in buffer order.
    pub fn nucleus_chars(&self) -> impl Iterator<Item = char> + Clone + 'a {
        self.rest.chars().filter(|&c| is_vowel(c))
    }

    /// The consonant coda, in buffer order.
    pub fn coda_chars(&self) -> impl Iterator<Item = char> + Clone + 'a {
        self.rest.chars().filter(|&c| !is_vowel(c))
    }

    /// True when the post-onset text is one vowel run followed by one consonant
    /// run — nucleus first, coda last, the only order a Vietnamese syllable has.
    /// The iterators above are order-blind on purpose, so without this a
    /// consonant stranded between onset and nucleus is misread as the coda:
    /// `cno` → rhyme `on`, `ona` → `oan`. Onset clusters (`kr`, `pl`) are already
    /// consumed by the onset, so `krông` and `plắk` stay well ordered.
    pub fn is_well_ordered(&self) -> bool {
        self.rest
            .chars()
            .skip_while(|&c| is_vowel(c))
            .all(|c| !is_vowel(c))
    }
}

/// Parse one syllable chunk into onset, vowel nucleus, and coda.
pub fn parse_syllable(buffer: &str) -> SyllableParts<'_> {
    let (onset, rest, invalid_onset) = onset::match_onset(buffer);
    SyllableParts {
        onset,
        rest,
        invalid_onset,
    }
}

#[cfg(test)]
mod tests;
