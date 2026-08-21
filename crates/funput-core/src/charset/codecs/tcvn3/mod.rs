//! TCVN3 (ABC) — the encoding of Vietnamese government documents, drawn by the
//! `.VnTime` font family.
//!
//! One byte per letter, Vietnamese letters in `0xA1..=0xFE`, ASCII below `0x80`
//! untouched. The codes themselves live in [`table`].
//!
//! # The one thing it cannot do
//!
//! TCVN3 encodes no uppercase toned vowel. `.VnTimeH` draws those from the very
//! codes this table gives their lowercase forms, so the byte carries the letter
//! and the *font* carries the case.
//!
//! Both directions lose by it. Encoding `Ề` writes `ề`'s code and reports the loss;
//! decoding can only ever produce lowercase, and no amount of cleverness recovers
//! what was never written down. Writing the nearest code beats refusing: the output
//! is going into a `.VnTime` document, where a correct letter in the wrong case is
//! something the user can fix and a raw `Ề` is silent garbage.

mod table;

use crate::charset::pivot::Atom;
use crate::charset::transcode::{Cursor, Decoded, Reading};

/// Read one TCVN3 byte at the cursor.
///
/// A high byte TCVN3 does not assign is carried through as itself, but reported
/// unrecognized. That matters more than it looks: the same code point is also a
/// perfectly good Latin-1 letter, so re-encoding it later can quietly land on a
/// *different* TCVN3 code (`0xC2` is unassigned, but reads back as `Â`, whose code
/// is `0xA2`). Flagging it is what turns that silent shift into a number the user
/// can see — and a high count is usually them having picked the wrong source
/// charset.
pub(super) fn decode(cur: &Cursor<'_>) -> Decoded {
    let c = cur.first();
    let (atom, reading) = match u8::try_from(c as u32) {
        Ok(byte) if byte >= 0x80 => match atom_for_byte(byte) {
            Some(atom) => (atom, Reading::Exact),
            None => (Atom::Other(c), Reading::Unknown),
        },
        // Below 0x80 TCVN3 is ASCII — but an ASCII vowel still has to pick up its
        // family coordinates, or the target charset could not re-spell it.
        _ => (Atom::from_char(c), Reading::Exact),
    };
    Decoded {
        atom,
        consumed: 1,
        reading,
    }
}

/// Write `atom` as TCVN3. `false` when the code written is not exact — which for
/// this charset means an uppercase toned vowel, or a character it has no code for.
pub(super) fn encode(atom: Atom, out: &mut String) -> bool {
    match atom {
        Atom::Vowel {
            family,
            tone,
            upper,
        } => encode_vowel(atom, family.into(), tone.into(), upper, out),
        Atom::Stroke { upper } => {
            out.push(byte_char(if upper {
                table::STROKE_UPPER
            } else {
                table::STROKE_LOWER
            }));
            true
        }
        // ASCII survives unchanged. Anything else has no TCVN3 code at all, so it
        // goes through as itself and is reported.
        Atom::Other(c) => {
            out.push(c);
            c.is_ascii()
        }
    }
}

fn encode_vowel(atom: Atom, family: usize, tone: usize, upper: bool, out: &mut String) -> bool {
    let byte = table::TCVN3_BYTES[family][tone];
    if byte == 0 {
        // A plain ASCII vowel; TCVN3 spells it as ASCII in either case.
        out.push(atom.to_char());
        return true;
    }
    if !upper {
        out.push(byte_char(byte));
        return true;
    }
    match upper_base(family).filter(|_| tone == 0) {
        // Ă Â Ê Ô Ơ Ư — the uppercase vowels TCVN3 does encode.
        Some(code) => {
            out.push(byte_char(code));
            true
        }
        // Uppercase and toned: the code is right, the case is lost with the font.
        None => {
            out.push(byte_char(byte));
            false
        }
    }
}

/// The uppercase code for a family's shaped base, for the six families that have
/// one. `None` for the ASCII-base families, whose uppercase is ASCII anyway.
fn upper_base(family: usize) -> Option<u8> {
    table::UPPER_BASES
        .iter()
        .find(|(shaped, _)| *shaped == family)
        .map(|&(_, code)| code)
}

/// The letter a TCVN3 byte stands for, or `None` when the byte is not one of the
/// 74 the charset assigns to Vietnamese.
fn atom_for_byte(byte: u8) -> Option<Atom> {
    match byte {
        table::STROKE_LOWER => return Some(Atom::Stroke { upper: false }),
        table::STROKE_UPPER => return Some(Atom::Stroke { upper: true }),
        _ => {}
    }
    if let Some(&(family, _)) = table::UPPER_BASES.iter().find(|(_, code)| *code == byte) {
        return Atom::vowel(family, 0, true);
    }
    let (family, row) = table::TCVN3_BYTES
        .iter()
        .enumerate()
        .find(|(_, row)| row.contains(&byte))?;
    let tone = row.iter().position(|&code| code == byte)?;
    Atom::vowel(family, tone, false)
}

/// A TCVN3 byte as the char a legacy document stores it as — see
/// [`crate::charset::decode_bytes`] for why the two are the same thing.
fn byte_char(byte: u8) -> char {
    char::from(byte)
}

#[cfg(test)]
mod tests;
