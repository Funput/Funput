//! The eight combining marks, and the one question every reader asks of them.
//!
//! Vietnamese has two ways to spell the same letter. `ế` is one code point in the
//! precomposed form that every modern system uses, and in the combining form it is
//! `e` followed by `U+0302` and `U+0301` — which is what NFD text from a macOS
//! filesystem or a web form actually contains. The inventory in [`super::vowels`]
//! covers the first spelling; these eight code points are the second.
//!
//! **Why they are here and not in the codec that reads them.** They are a fact about
//! the language, not about an encoding: NFD text turns up whether or not anyone is
//! converting a charset. The charset codec that decodes Unicode tổ hợp is one reader
//! of them, and `textcase` is another — bỏ dấu drops a mark without knowing what a
//! charset is. A copy per reader would put the eight code points in two places, and
//! the second place is the one that goes stale.
//!
//! Code points checked against the canonical NFD of the whole Vietnamese inventory,
//! taken from a Unicode normalizer rather than from memory.
//!
//! What is *not* here is what a mark means to a particular encoding: whether it can
//! be written as well as read, and what it does to the letter before it. That stays
//! with each reader, because the answers differ — the UniKey convention keeps
//! `ă â ê ô ơ ư` precomposed, so the charset codec reads the three shape marks and
//! never emits one.

use super::marks::Tone;
use super::shapes::VowelShape;

/// The combining code point that writes each tone, in the inventory's own order:
/// sắc, huyền, hỏi, ngã, nặng.
pub(crate) const TONES: [(char, Tone); 5] = [
    ('\u{301}', Tone::Sac),
    ('\u{300}', Tone::Huyen),
    ('\u{309}', Tone::Hoi),
    ('\u{303}', Tone::Nga),
    ('\u{323}', Tone::Nang),
];

/// The combining code point that writes each vowel shape.
pub(crate) const SHAPES: [(char, VowelShape); 3] = [
    ('\u{302}', VowelShape::Circumflex),
    ('\u{306}', VowelShape::Breve),
    ('\u{31B}', VowelShape::Horn),
];

/// Whether `c` is one of the eight — a mark that modifies the letter before it
/// instead of being a letter in its own right.
pub(crate) fn is_mark(c: char) -> bool {
    TONES.iter().any(|&(mark, _)| mark == c) || SHAPES.iter().any(|&(mark, _)| mark == c)
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A transcribed code point is invisible to the eye, so pin the tables: every
    /// tone and every shape appears exactly once, and no two marks share a code
    /// point. What each mark *means* to a charset is tested with the codec that
    /// reads them.
    #[test]
    fn the_eight_marks_are_distinct_and_complete() {
        for tone in [Tone::Sac, Tone::Huyen, Tone::Hoi, Tone::Nga, Tone::Nang] {
            assert_eq!(
                TONES.iter().filter(|&&(_, t)| t == tone).count(),
                1,
                "{tone:?}"
            );
        }
        for shape in [VowelShape::Circumflex, VowelShape::Breve, VowelShape::Horn] {
            assert_eq!(
                SHAPES.iter().filter(|&&(_, s)| s == shape).count(),
                1,
                "{shape:?}"
            );
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
        assert!(points.iter().copied().all(is_mark));
    }

    #[test]
    fn a_letter_is_not_a_mark() {
        for c in ['a', 'â', 'ơ', 'đ', 'Đ', '₫', ' '] {
            assert!(!is_mark(c), "{c}");
        }
    }
}
