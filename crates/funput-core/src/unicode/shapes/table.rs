//! The shaped-vowel table: which glyph carries which shape, and over which base.
//!
//! Kept as data next to one lookup so the operations in the parent module read as
//! rules rather than as glyph lists.

use super::VowelShape;

pub(super) fn case_pair(base: char, lower: char, upper: char) -> char {
    if base.is_uppercase() { upper } else { lower }
}

pub(super) struct ShapedVowel {
    pub(super) shaped_lower: char,
    pub(super) base_lower: char,
    pub(super) shape: VowelShape,
}

const SHAPED_VOWELS: &[ShapedVowel] = &[
    ShapedVowel {
        shaped_lower: 'â',
        base_lower: 'a',
        shape: VowelShape::Circumflex,
    },
    ShapedVowel {
        shaped_lower: 'ă',
        base_lower: 'a',
        shape: VowelShape::Breve,
    },
    ShapedVowel {
        shaped_lower: 'ê',
        base_lower: 'e',
        shape: VowelShape::Circumflex,
    },
    ShapedVowel {
        shaped_lower: 'ô',
        base_lower: 'o',
        shape: VowelShape::Circumflex,
    },
    ShapedVowel {
        shaped_lower: 'ơ',
        base_lower: 'o',
        shape: VowelShape::Horn,
    },
    ShapedVowel {
        shaped_lower: 'ư',
        base_lower: 'u',
        shape: VowelShape::Horn,
    },
];

pub(super) fn shaped_entry(shaped_stem: char) -> Option<&'static ShapedVowel> {
    let lower = char::to_lowercase(shaped_stem)
        .next()
        .unwrap_or(shaped_stem);
    SHAPED_VOWELS
        .iter()
        .find(|entry| entry.shaped_lower == lower)
}
