//! The VNI-Windows spelling of every letter in the Vietnamese inventory.
//!
//! # Where these numbers came from
//!
//! Read as raw bytes out of two converter implementations — `anhskohbo/u-convert`
//! (PHP) and `vuthaihoc/py-unicode-convert` (Python) — and compared entry by entry:
//! **134 of 134 agree, case-sensitively, with no cell differing.** Three
//! independent anchors line up with them: vietunicode states `á = 97,249`
//! (`61 F9`), and a separate gist gives `Ấ = 41 C1`, `Ằ = 41 C8`, `Ẳ = 41 DA`.
//!
//! Those two implementations share a lineage, so the comparison proves faithful
//! transcription rather than two independent derivations. What actually proves the
//! table is the structure below, which `tests.rs` asserts in full.
//!
//! # Structure
//!
//! A letter is **a base byte and an optional mark byte**, in that order — one
//! model for all seventy-two, including the ones that come out a single byte
//! (`(0xED, 0)` is `í`) and the two whose base is not ASCII (`(0xF4, …)` is the `ơ`
//! family). Storing the base in full rather than "0 = derive it from the family"
//! is what keeps the reverse lookup a flat scan and gives the distinctness check
//! below something to bite on.
//!
//! Three mark sets do all the work: plain `F9 F8 FB F5 EF`, circumflex `E2` plus
//! `E1 E0 E5 E3 E4` (shared by `â ê ô`, told apart by the base), and breve `EA`
//! plus `E9 E8 FA FC EB` for `ă` alone.
//!
//! **Uppercase is every byte minus `0x20`**, without exception — so this table
//! holds only lowercase and half the charset is derived rather than transcribed.

use crate::unicode::vowels;

/// A letter's spelling: base byte, then mark byte (`0` = the letter is one byte).
pub(super) type Cell = (u8, u8);

/// VNI-Windows spelling per (family, slot) of the vowel inventory.
///
/// Rows follow `VOWEL_FAMILIES`, so the index is shared with the inventory rather
/// than re-derived. Columns follow it too: base, sắc, huyền, hỏi, ngã, nặng.
///
/// Two rows break the base-plus-mark rhythm, and both are VNI's doing rather than
/// this table's: the `i` family spells every tone as **one** byte, and `ỵ` alone
/// does the same in a family whose other tones do not.
///
/// `rustfmt` would put every tuple on its own line, turning twelve readable rows
/// into a hundred and fifty. A charset table is read as a grid or not at all.
#[rustfmt::skip]
pub(super) const VNI_BYTES: [[Cell; 6]; 12] = [
    /* a */ [(0x61, 0x00), (0x61, 0xF9), (0x61, 0xF8), (0x61, 0xFB), (0x61, 0xF5), (0x61, 0xEF)],
    /* ă */ [(0x61, 0xEA), (0x61, 0xE9), (0x61, 0xE8), (0x61, 0xFA), (0x61, 0xFC), (0x61, 0xEB)],
    /* â */ [(0x61, 0xE2), (0x61, 0xE1), (0x61, 0xE0), (0x61, 0xE5), (0x61, 0xE3), (0x61, 0xE4)],
    /* e */ [(0x65, 0x00), (0x65, 0xF9), (0x65, 0xF8), (0x65, 0xFB), (0x65, 0xF5), (0x65, 0xEF)],
    /* ê */ [(0x65, 0xE2), (0x65, 0xE1), (0x65, 0xE0), (0x65, 0xE5), (0x65, 0xE3), (0x65, 0xE4)],
    /* i */ [(0x69, 0x00), (0xED, 0x00), (0xEC, 0x00), (0xE6, 0x00), (0xF3, 0x00), (0xF2, 0x00)],
    /* o */ [(0x6F, 0x00), (0x6F, 0xF9), (0x6F, 0xF8), (0x6F, 0xFB), (0x6F, 0xF5), (0x6F, 0xEF)],
    /* ô */ [(0x6F, 0xE2), (0x6F, 0xE1), (0x6F, 0xE0), (0x6F, 0xE5), (0x6F, 0xE3), (0x6F, 0xE4)],
    /* ơ */ [(0xF4, 0x00), (0xF4, 0xF9), (0xF4, 0xF8), (0xF4, 0xFB), (0xF4, 0xF5), (0xF4, 0xEF)],
    /* u */ [(0x75, 0x00), (0x75, 0xF9), (0x75, 0xF8), (0x75, 0xFB), (0x75, 0xF5), (0x75, 0xEF)],
    /* ư */ [(0xF6, 0x00), (0xF6, 0xF9), (0xF6, 0xF8), (0xF6, 0xFB), (0xF6, 0xF5), (0xF6, 0xEF)],
    /* y */ [(0x79, 0x00), (0x79, 0xF9), (0x79, 0xF8), (0x79, 0xFB), (0x79, 0xF5), (0xEE, 0x00)],
];

/// A thirteenth vowel family would leave this table silently short.
const _: () = assert!(VNI_BYTES.len() == vowels::FAMILY_COUNT);

/// Every letter has a base. `0` is the sentinel for a missing *mark*, and reusing
/// it for a missing base would make "no base" and "ASCII base" the same value.
/// The floor keeps `base - CASE_OFFSET` inside the printable range.
const _: () = {
    let mut family = 0;
    while family < VNI_BYTES.len() {
        let mut tone = 0;
        while tone < 6 {
            assert!(
                VNI_BYTES[family][tone].0 >= 0x61,
                "base byte missing or too low"
            );
            tone += 1;
        }
        family += 1;
    }
};

/// `đ` — one byte, no mark, and no family to live in.
pub(super) const STROKE_LOWER: u8 = 0xF1;

/// How far above its uppercase counterpart every VNI byte sits. Holds for all
/// sixty-six vowel spellings and for `đ` (`Đ` = `0xD1`), which is why no uppercase
/// byte is written down anywhere in this file.
pub(super) const CASE_OFFSET: u8 = 0x20;
