//! What each of the eight combining marks means to this charset.
//!
//! The code points themselves live in [`crate::unicode::combining`]: they are a fact
//! about the language, and this codec is one reader of them rather than their owner.
//! What is here is the part that *is* about the encoding — [`Mark`], which says what
//! a mark adds to the letter before it, and the asymmetry below.
//!
//! Tones are read *and* written. Shapes are **read only**: the UniKey convention
//! keeps `ă â ê ô ơ ư` precomposed, so [`super::encode`] never emits one. They are
//! still read because real NFD text — from a macOS filesystem, from a web form —
//! does contain them, and refusing to read it would help nobody.

use crate::unicode::combining;
use crate::unicode::marks::Tone;
use crate::unicode::shapes::VowelShape;

/// What a combining mark adds to the letter before it.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(super) enum Mark {
    Tone(Tone),
    Shape(VowelShape),
}

/// What `c` adds to the letter before it, or `None` when it is not one of the
/// eight — in which case it is a letter in its own right, not a mark.
pub(super) fn mark_of(c: char) -> Option<Mark> {
    if let Some(&(_, tone)) = combining::TONES.iter().find(|&&(mark, _)| mark == c) {
        return Some(Mark::Tone(tone));
    }
    let &(_, shape) = combining::SHAPES.iter().find(|&&(mark, _)| mark == c)?;
    Some(Mark::Shape(shape))
}

/// The mark that writes `tone`.
pub(super) fn tone_mark(tone: Tone) -> char {
    combining::TONES
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
    combining::is_mark(c)
}

#[cfg(test)]
mod tests {
    use super::*;

    /// A transcribed code point is invisible to the eye, and [`mark_of`] is what
    /// turns one into meaning, so pin the whole map: every tone and every shape
    /// round-trips through it. That the eight code points are distinct is the
    /// tables' own business, and tested where they live.
    #[test]
    fn the_map_covers_every_tone_and_shape() {
        for tone in [Tone::Sac, Tone::Huyen, Tone::Hoi, Tone::Nga, Tone::Nang] {
            assert_eq!(mark_of(tone_mark(tone)), Some(Mark::Tone(tone)));
        }
        for (mark, shape) in combining::SHAPES {
            assert_eq!(mark_of(mark), Some(Mark::Shape(shape)));
        }
    }

    #[test]
    fn a_letter_is_not_a_mark() {
        for c in ['a', 'â', 'ơ', 'đ', 'Đ', '₫', ' '] {
            assert!(!is_mark(c), "{c}");
            assert_eq!(mark_of(c), None, "{c}");
        }
    }
}
