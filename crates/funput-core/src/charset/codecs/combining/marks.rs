//! The eight combining marks this charset reads, and the five it writes.
//!
//! Code points checked against the canonical NFD of the whole Vietnamese
//! inventory, taken from a Unicode normalizer rather than from memory.
//!
//! Tones are read *and* written. Shapes are **read only**: the UniKey convention
//! keeps `ă â ê ô ơ ư` precomposed, so [`super::encode`] never emits one. They are
//! listed here because real NFD text — from a macOS filesystem, from a web form —
//! does contain them, and refusing to read it would help nobody.

use crate::unicode::marks::Tone;
use crate::unicode::shapes::VowelShape;

/// What a combining mark adds to the letter before it.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(super) enum Mark {
    Tone(Tone),
    Shape(VowelShape),
}

/// Tone marks, in the inventory's own order: sắc, huyền, hỏi, ngã, nặng.
const TONES: [(char, Tone); 5] = [
    ('\u{301}', Tone::Sac),
    ('\u{300}', Tone::Huyen),
    ('\u{309}', Tone::Hoi),
    ('\u{303}', Tone::Nga),
    ('\u{323}', Tone::Nang),
];

/// Shape marks. Read-only — see the module doc.
const SHAPES: [(char, VowelShape); 3] = [
    ('\u{302}', VowelShape::Circumflex),
    ('\u{306}', VowelShape::Breve),
    ('\u{31B}', VowelShape::Horn),
];

/// What `c` adds to the letter before it, or `None` when it is not one of the
/// eight — in which case it is a letter in its own right, not a mark.
pub(super) fn mark_of(c: char) -> Option<Mark> {
    if let Some(&(_, tone)) = TONES.iter().find(|&&(mark, _)| mark == c) {
        return Some(Mark::Tone(tone));
    }
    let &(_, shape) = SHAPES.iter().find(|&&(mark, _)| mark == c)?;
    Some(Mark::Shape(shape))
}

/// The mark that writes `tone`.
pub(super) fn tone_mark(tone: Tone) -> char {
    TONES
        .iter()
        .find(|&&(_, candidate)| candidate == tone)
        .map(|&(mark, _)| mark)
        .expect("every tone has a combining mark")
}

/// Whether `c` is one of the eight.
///
/// [`super::encode`] needs this to refuse a passthrough that would fuse with the
/// letter before it: writing a bare `U+0301` after an `a` produces text that reads
/// back as `á`, which is a different string.
pub(super) fn is_mark(c: char) -> bool {
    mark_of(c).is_some()
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A transcribed code point is invisible to the eye, so pin the whole map:
    /// every tone and every shape is present exactly once, and no code point does
    /// double duty.
    #[test]
    fn the_map_covers_every_tone_and_shape_exactly_once() {
        for tone in [Tone::Sac, Tone::Huyen, Tone::Hoi, Tone::Nga, Tone::Nang] {
            assert_eq!(mark_of(tone_mark(tone)), Some(Mark::Tone(tone)));
        }
        for shape in [VowelShape::Circumflex, VowelShape::Breve, VowelShape::Horn] {
            let found = SHAPES.iter().filter(|&&(_, s)| s == shape).count();
            assert_eq!(found, 1, "{shape:?}");
        }

        let mut points: Vec<char> = TONES
            .iter()
            .map(|&(mark, _)| mark)
            .chain(SHAPES.iter().map(|&(mark, _)| mark))
            .collect();
        assert_eq!(points.len(), 8);
        points.sort_unstable();
        points.dedup();
        assert_eq!(points.len(), 8, "two marks share a code point");
    }

    #[test]
    fn a_letter_is_not_a_mark() {
        for c in ['a', 'â', 'ơ', 'đ', 'Đ', '₫', ' '] {
            assert!(!is_mark(c), "{c}");
        }
    }
}
