//! Unicode tổ hợp — UniKey's convention, where the tone rides as a combining mark
//! and the vowel keeps its shape.
//!
//! `ậ` is `â` plus `U+0323`; `ắ` is `ă` plus `U+0301`; `ơ` and `đ` are themselves.
//! This is **not** NFD, which would take the shape apart too (`ậ` = `a` `U+0323`
//! `U+0302`). The marks live in [`marks`], the letter-building in [`compose`].
//!
//! # Lenient in, strict out
//!
//! Reading accepts the UniKey spelling, full NFD, a precomposed letter, and the
//! marks in **either order** — NFD's own order flips depending on the tone, so
//! order-independence is less work *and* more correct than following the combining
//! classes. Writing only ever produces the UniKey spelling.
//!
//! Anything read but rewritten is reported as `normalized`, not `unmapped`:
//! nothing was lost and nothing was guessed, only the spelling changed. Reading
//! ordinary precomposed Vietnamese this way therefore reports roughly one per
//! toned letter — correct, and worth knowing before `detect` is written.
//!
//! # What it cannot spell
//!
//! Nothing. It is Unicode, so `₫` and `日` come through exactly, which no legacy
//! charset can claim. The one refusal is a bare combining mark arriving as a
//! passthrough: writing it after a letter would fuse the two, and the text would
//! read back as something else.

mod compose;
mod marks;

use crate::charset::pivot::Atom;
use crate::charset::transcode::{Cursor, Decoded, Reading};

use compose::Letter;

/// How many marks a letter can carry: one shape and one tone. NFD never needs
/// more, and without a ceiling a base followed by sixty marks would read as one
/// letter and hide fifty-nine of them.
const MAX_MARKS: usize = 2;

/// Read one letter at the cursor, taking the marks that belong to it.
pub(super) fn decode(cur: &Cursor<'_>) -> Decoded {
    let first = cur.first();
    if marks::is_mark(first) {
        // A mark with no letter before it. `Atom::from_char` would be wrong here:
        // it classifies, and there is nothing to classify — this is debris.
        return Decoded {
            atom: Atom::Other(first),
            consumed: 1,
            reading: Reading::Unknown,
        };
    }

    let mut letter = Letter::seed(first);
    let mut consumed = 1;
    for next in cur.following().take(MAX_MARKS) {
        let Some(mark) = marks::mark_of(next) else {
            break;
        };
        if !letter.absorb(mark) {
            break;
        }
        consumed += 1;
    }

    Decoded {
        atom: Atom::from_char(letter.compose()),
        consumed,
        reading: if letter.is_canonical() {
            Reading::Exact
        } else {
            Reading::Rewritten
        },
    }
}

/// Write `atom` the way UniKey writes it: the shaped vowel precomposed, the tone
/// as its own mark.
pub(super) fn encode(atom: Atom, out: &mut String) -> bool {
    match atom {
        Atom::Vowel {
            family,
            tone,
            upper,
        } => {
            let stem = Atom::vowel(family.into(), 0, upper)
                .expect("a family always has a toneless slot")
                .to_char();
            out.push(stem);
            if let Some(tone) = tone_of(tone) {
                out.push(marks::tone_mark(tone));
            }
            true
        }
        // `đ` has no canonical decomposition; there is nothing to take apart.
        Atom::Stroke { .. } => {
            out.push(atom.to_char());
            true
        }
        // Unicode holds anything — except one of this charset's own marks, which
        // would attach itself to the letter before it and change what the text
        // says. `₫` is exact here; a stray `U+0301` is not.
        Atom::Other(c) => {
            out.push(c);
            !marks::is_mark(c)
        }
    }
}

/// The tone an [`Atom`]'s slot index stands for: 0 is toneless, 1–5 are the tones
/// in the inventory's order.
fn tone_of(slot: u8) -> Option<crate::unicode::marks::Tone> {
    use crate::unicode::marks::Tone;
    match slot {
        1 => Some(Tone::Sac),
        2 => Some(Tone::Huyen),
        3 => Some(Tone::Hoi),
        4 => Some(Tone::Nga),
        5 => Some(Tone::Nang),
        _ => None,
    }
}

#[cfg(test)]
mod tests;
