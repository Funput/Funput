//! VNI-Windows — the encoding drawn by the `VNI-Times` font family.
//!
//! A letter is a **base byte plus an optional mark byte**, so unlike TCVN3 one
//! letter is not one char: `Ề` goes out as two, and conversion moves character
//! boundaries. The spellings live in [`table`], the lookups in [`bytes`].
//!
//! # Reading is greedy, and that is provably safe
//!
//! Decoding takes the longest match: try the two-byte spelling first, then the
//! one-byte one. It is safe because **no mark byte is ever a letter on its own** —
//! the bases and the marks are disjoint sets. So a pair that matches could not have
//! been two letters, and there is no ordering hazard to reason about. `tests.rs`
//! asserts the disjointness rather than leaving it as a claim.
//!
//! The naive alternative — handle one-byte letters first — looks equivalent and is
//! not: `0xF4` is both the letter `ơ` and the base of all five of its toned forms,
//! so `ơ` would be taken alone and every `ớ ờ ở ỡ ợ` would break in half.
//!
//! # Two things it refuses to guess
//!
//! **A mark whose case disagrees with its base** (`a` + the uppercase sắc byte) is
//! not something VNI emits, but sloppy converters do. It is read as the letter, in
//! the base's case, and counted — the reading a person would give, without
//! inventing a stray Latin-1 vowel that would then grow into two more bytes.
//!
//! **The `i` family is one byte per tone**, never base-plus-mark. Some converters
//! in the wild write `í` as `i` plus the sắc mark; that is read here as `i`
//! followed by an orphan, and counted. Accepting both would give one letter two
//! spellings and cost the inventory sweep its power to catch a bad table.
//!
//! # What it cannot spell
//!
//! Nothing, within Vietnamese — including every uppercase toned vowel, where TCVN3
//! gives up. `encode` returns false only for a non-ASCII character that was never
//! Vietnamese to begin with.

mod bytes;
mod table;

use crate::charset::pivot::Atom;
use crate::charset::transcode::{Cursor, Decoded, Reading};

use table::STROKE_LOWER;

/// Read one VNI letter at the cursor, taking two chars where the spelling has two.
pub(super) fn decode(cur: &Cursor<'_>) -> Decoded {
    let first = cur.first();
    let Some((base, base_upper)) = bytes::byte_of(first).and_then(bytes::fold) else {
        return passthrough(first);
    };

    if base == STROKE_LOWER {
        return Decoded {
            atom: Atom::Stroke { upper: base_upper },
            consumed: 1,
            reading: Reading::Exact,
        };
    }

    // The mark, only if there is a second char *and* it is a byte at all. Folding
    // a missing char to zero would match the one-byte spellings and swallow it.
    let mark = cur.second().and_then(bytes::byte_of).and_then(bytes::fold);
    if let Some((mark_byte, mark_upper)) = mark
        && let Some((family, tone)) = bytes::coords_for(base, mark_byte)
        && let Some(atom) = Atom::vowel(family, tone, base_upper)
    {
        return Decoded {
            atom,
            consumed: 2,
            // VNI never writes a pair whose halves disagree on case.
            reading: if base_upper == mark_upper {
                Reading::Exact
            } else {
                Reading::Unknown
            },
        };
    }

    match bytes::coords_for(base, 0).and_then(|(f, t)| Atom::vowel(f, t, base_upper)) {
        Some(atom) => Decoded {
            atom,
            consumed: 1,
            reading: Reading::Exact,
        },
        None => passthrough(first),
    }
}

/// A char VNI does not spell. ASCII is still classified, so an ASCII vowel keeps
/// the family coordinates another charset would need to re-spell it.
fn passthrough(c: char) -> Decoded {
    match u8::try_from(c as u32) {
        Ok(byte) if byte >= 0x80 => Decoded {
            atom: Atom::Other(c),
            consumed: 1,
            reading: Reading::Unknown,
        },
        _ => Decoded {
            atom: Atom::from_char(c),
            consumed: 1,
            reading: Reading::Exact,
        },
    }
}

/// Write `atom` as VNI. `false` only for a non-ASCII character VNI has no code
/// for — every Vietnamese letter is spellable, in either case.
pub(super) fn encode(atom: Atom, out: &mut String) -> bool {
    match atom {
        Atom::Vowel {
            family,
            tone,
            upper,
        } => {
            let (base, mark) = bytes::spelling(family.into(), tone.into());
            out.push(bytes::cased(base, upper));
            if mark != 0 {
                out.push(bytes::cased(mark, upper));
            }
            true
        }
        Atom::Stroke { upper } => {
            out.push(bytes::cased(STROKE_LOWER, upper));
            true
        }
        // A passthrough whose code point happens to be a mark byte will fuse with
        // the letter before it when the text is read back. `unmapped` counts it,
        // so the module's "exact means reversible" contract still holds.
        Atom::Other(c) => {
            out.push(c);
            c.is_ascii()
        }
    }
}

#[cfg(test)]
mod tests;
