//! The TCVN3 code for every letter in the Vietnamese inventory.
//!
//! # Where these numbers came from
//!
//! Transcribed from three independent converter implementations and cross-checked
//! cell by cell — a Python one (`gist.github.com/congkhoa/bcd46eea…`, which lists
//! the pairs in its own order) and two PHP ones (`gist.github.com/ducviet1696/
//! 7a94421951532edc60cdc008ca989c41` and `github.com/anhskohbo/u-convert`). All
//! three agree on all 74 assignments.
//!
//! Two structural checks back that up, because a transposed byte is invisible to
//! the eye and produces mojibake only on real text: the 74 codes are pairwise
//! distinct, and they fill exactly twelve families of five tones plus the fourteen
//! shaped bases. `tests.rs` asserts both, and sweeps the whole inventory.
//!
//! # Lookup direction
//!
//! Reverse lookup (code → letter) is a linear scan of these 72 cells, not a dense
//! compile-time table like the one in [`crate::unicode::vowels`]. That one is
//! built because it runs once per keystroke; conversion runs once per paste, where
//! scanning is free — and keeping the data in one readable block next to its only
//! reader is worth more than the nanoseconds. Please do not "optimize" it back.

use crate::unicode::vowels;

/// TCVN3 code per (family, slot) of the vowel inventory; `0` where the letter is
/// plain ASCII and TCVN3 spells it as ASCII too.
///
/// Rows follow `VOWEL_FAMILIES`, so the index is shared with the inventory rather
/// than re-derived. Columns follow it too: base, sắc, huyền, hỏi, ngã, nặng.
/// TCVN3's own codes run in a different order inside each family — huyền, hỏi,
/// ngã, sắc, nặng, visible as `B5..B9` on the `a` row. That reordering lives here,
/// in the data, and nowhere in the code.
pub(super) const TCVN3_BYTES: [[u8; 6]; 12] = [
    /* a */ [0x00, 0xB8, 0xB5, 0xB6, 0xB7, 0xB9],
    /* ă */ [0xA8, 0xBE, 0xBB, 0xBC, 0xBD, 0xC6],
    /* â */ [0xA9, 0xCA, 0xC7, 0xC8, 0xC9, 0xCB],
    /* e */ [0x00, 0xD0, 0xCC, 0xCE, 0xCF, 0xD1],
    /* ê */ [0xAA, 0xD5, 0xD2, 0xD3, 0xD4, 0xD6],
    /* i */ [0x00, 0xDD, 0xD7, 0xD8, 0xDC, 0xDE],
    /* o */ [0x00, 0xE3, 0xDF, 0xE1, 0xE2, 0xE4],
    /* ô */ [0xAB, 0xE8, 0xE5, 0xE6, 0xE7, 0xE9],
    /* ơ */ [0xAC, 0xED, 0xEA, 0xEB, 0xEC, 0xEE],
    /* u */ [0x00, 0xF3, 0xEF, 0xF1, 0xF2, 0xF4],
    /* ư */ [0xAD, 0xF8, 0xF5, 0xF6, 0xF7, 0xF9],
    /* y */ [0x00, 0xFD, 0xFA, 0xFB, 0xFC, 0xFE],
];

/// A thirteenth vowel family would leave this table silently short. Make it a
/// build failure instead.
const _: () = assert!(TCVN3_BYTES.len() == vowels::FAMILY_COUNT);

/// Uppercase codes for the six shaped bases, by family index.
///
/// These are the *only* uppercase vowels TCVN3 encodes. Every toned uppercase
/// vowel — `Ầ`, `Ế`, `Ự` — has no code at all: a TCVN3 document draws those by
/// switching to the `.VnTimeH` font, which maps these same lowercase codes to
/// uppercase glyphs. The charset carries no case information, and no
/// implementation can invent it.
pub(super) const UPPER_BASES: [(usize, u8); 6] = [
    (1, 0xA1),  // Ă
    (2, 0xA2),  // Â
    (4, 0xA3),  // Ê
    (7, 0xA4),  // Ô
    (8, 0xA5),  // Ơ
    (10, 0xA6), // Ư
];

/// `đ` — the one consonant TCVN3 gives a code of its own.
pub(super) const STROKE_LOWER: u8 = 0xAE;
/// `Đ`, which unlike the toned vowels does have its own code.
pub(super) const STROKE_UPPER: u8 = 0xA7;
