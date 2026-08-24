//! The pivot: one unit of text with its encoding stripped off.
//!
//! Every conversion runs `source → Atom → target`, so a charset only ever has to
//! know how to spell an [`Atom`] — never how any *other* charset spells one. That
//! is what keeps the cost of charset N+1 at two mappings instead of N.
//!
//! Three variants cover all four charsets Funput targets. The worked mapping table
//! in `docs/features/charset.md` is where that claim is checked letter by letter.

use crate::unicode::vowels;

/// One unit of text, independent of how a charset spells it.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(super) enum Atom {
    /// A vowel by its inventory coordinates: `family` indexes the vowel families
    /// of [`crate::unicode::vowels`], and `tone` is 0 for the toneless base and
    /// 1–5 for sắc, huyền, hỏi, ngã, nặng.
    ///
    /// Build one with [`Atom::vowel`], never by writing the fields — that is what
    /// lets [`Atom::to_char`] be infallible.
    Vowel { family: u8, tone: u8, upper: bool },
    /// `đ`/`Đ`. The one consonant every legacy Vietnamese encoding gives a code of
    /// its own, so it cannot ride along in [`Atom::Other`].
    Stroke { upper: bool },
    /// Everything else: consonants, digits, punctuation, foreign letters. Carried
    /// through untouched — a charset that cannot spell it says so when it encodes.
    Other(char),
}

impl Atom {
    /// Classify one precomposed Unicode char. Total: anything outside the
    /// Vietnamese inventory becomes [`Atom::Other`], which is why decoding never
    /// has to report failure.
    pub(super) fn from_char(c: char) -> Self {
        if let Some((family, tone, upper)) = vowels::vowel_coords(c) {
            return Self::Vowel {
                family: family as u8,
                tone: tone as u8,
                upper,
            };
        }
        match c {
            'đ' => Self::Stroke { upper: false },
            'Đ' => Self::Stroke { upper: true },
            other => Self::Other(other),
        }
    }

    /// Build a vowel atom from inventory coordinates, or `None` when the inventory
    /// has no such letter. The only constructor for [`Atom::Vowel`], so a codec
    /// cannot hand [`Atom::to_char`] coordinates that do not exist.
    pub(super) fn vowel(family: usize, tone: usize, upper: bool) -> Option<Self> {
        vowels::vowel_glyph(family, tone, upper)?;
        Some(Self::Vowel {
            family: family as u8,
            tone: tone as u8,
            upper,
        })
    }

    /// The precomposed Unicode char this atom stands for.
    pub(super) fn to_char(self) -> char {
        match self {
            Self::Vowel {
                family,
                tone,
                upper,
            } => vowels::vowel_glyph(family.into(), tone.into(), upper)
                .expect("Atom::vowel checks its coordinates against the inventory"),
            Self::Stroke { upper } => {
                if upper {
                    'Đ'
                } else {
                    'đ'
                }
            }
            Self::Other(c) => c,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn every_vietnamese_letter_round_trips_through_an_atom() {
        for family in 0..vowels::FAMILY_COUNT {
            for tone in 0..6 {
                for upper in [false, true] {
                    let c = vowels::vowel_glyph(family, tone, upper).expect("in inventory");
                    assert_eq!(Atom::from_char(c).to_char(), c, "{c}");
                }
            }
        }
        for c in ['đ', 'Đ'] {
            assert_eq!(Atom::from_char(c).to_char(), c, "{c}");
        }
    }

    #[test]
    fn a_stroke_is_not_an_other() {
        assert_eq!(Atom::from_char('đ'), Atom::Stroke { upper: false });
        assert_eq!(Atom::from_char('Đ'), Atom::Stroke { upper: true });
    }

    #[test]
    fn anything_outside_the_inventory_passes_through() {
        for c in ['b', 'Z', '7', ' ', '₫', '日'] {
            assert_eq!(Atom::from_char(c), Atom::Other(c), "{c}");
            assert_eq!(Atom::from_char(c).to_char(), c, "{c}");
        }
    }

    #[test]
    fn coordinates_outside_the_inventory_are_refused() {
        assert!(Atom::vowel(vowels::FAMILY_COUNT, 0, false).is_none());
        assert!(Atom::vowel(0, 6, false).is_none());
        assert!(Atom::vowel(0, 0, false).is_some());
    }
}
