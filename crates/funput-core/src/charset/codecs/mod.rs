//! Where a charset says how it spells an [`Atom`] — the extension point — and the
//! byte layer that feeds it.
//!
//! The second half is here because it is the same question asked one level down:
//! *how does this charset put itself in a file?* [`decode_bytes`] and
//! [`is_byte_oriented`] are public, and the two UTF-8 helpers below them exist only
//! to serve the first.
//!
//! Adding a charset means adding a codec module here plus a [`Charset`] variant.
//! The pivot, the driver, and every consumer stay as they are, and the compiler
//! points at the match arms you owe: these three matches are exhaustive over
//! `Charset`, so a new variant cannot be forgotten halfway.
//!
//! No trait, deliberately. `funput-core` dispatches on enums throughout (see
//! [`crate::input_method`]) and defines no traits at all; a `dyn` codec would add
//! a vtable and take away exactly the exhaustiveness that makes this safe to
//! extend. `#[non_exhaustive]` on `Charset` does not weaken it — it only affects
//! callers outside the crate.
//!
//! Each codec provides two functions:
//!
//! - `decode(&Cursor) -> Decoded` — read one unit at the cursor. Total: anything
//!   the charset does not define comes back as [`Atom::Other`] with a `reading` of
//!   [`Reading::Unknown`], never as an error.
//! - `encode(Atom, &mut String) -> bool` — write the atom, returning `false` when
//!   what it wrote is not exact. It always writes *something*; a character never
//!   disappears in conversion.

mod combining;
mod tcvn3;
mod vni;

use super::Charset;
use super::pivot::Atom;
use super::transcode::{Conversion, Cursor, Decoded, Reading};

/// Read one unit of `charset` at the cursor.
pub(super) fn decode(charset: Charset, cur: &Cursor<'_>) -> Decoded {
    match charset {
        // The pivot is already the atom's own spelling — nothing to undo, and no
        // char it fails to define.
        Charset::Unicode => Decoded {
            atom: Atom::from_char(cur.first()),
            consumed: 1,
            reading: Reading::Exact,
        },
        Charset::Tcvn3 => tcvn3::decode(cur),
        Charset::VniWindows => vni::decode(cur),
        Charset::UnicodeCombining => combining::decode(cur),
    }
}

/// Write `atom` as `charset` spells it. `false` means the spelling is not exact —
/// see the module doc.
pub(super) fn encode(charset: Charset, atom: Atom, out: &mut String) -> bool {
    match charset {
        Charset::Unicode => {
            out.push(atom.to_char());
            true
        }
        Charset::Tcvn3 => tcvn3::encode(atom, out),
        Charset::VniWindows => vni::encode(atom, out),
        Charset::UnicodeCombining => combining::encode(atom, out),
    }
}

/// Whether a charset stores one byte per character, drawn by a legacy font.
///
/// The distinction a caller needs when it already knows something about the bytes.
/// Text that failed a UTF-8 decode cannot be in a charset that is *not* byte
/// oriented, so a caller detecting the charset of such bytes must refuse those
/// candidates — feeding them to [`decode_bytes`] would hand back replacement
/// characters, which is a worse answer than admitting defeat.
///
/// Asking this instead of naming charsets is what lets a consumer keep working when
/// a new one is added.
pub fn is_byte_oriented(charset: Charset) -> bool {
    match charset {
        Charset::Unicode | Charset::UnicodeCombining => false,
        Charset::Tcvn3 | Charset::VniWindows => true,
    }
}

/// Read raw bytes in `from` and return them as Unicode.
///
/// A byte-oriented charset's bytes are read one-to-one as code points, which is
/// exactly how the document that produced them was stored. The rest are decoded as
/// UTF-8, where an invalid sequence becomes `U+FFFD` — a caller reading a file it
/// merely *believes* is UTF-8 gets told when it was wrong instead of silently
/// receiving replacement characters.
///
/// Either way the text then goes through [`convert`], and the two counts add up:
/// broken bytes and characters the charset could not place are separate problems,
/// and a byte that causes both deserves two looks.
pub fn decode_bytes(bytes: &[u8], from: Charset) -> Conversion {
    let (text, damage) = if is_byte_oriented(from) {
        // Latin-1 is total, so reading bytes as code points cannot fail.
        (bytes.iter().copied().map(char::from).collect(), 0)
    } else {
        let text = String::from_utf8_lossy(bytes).into_owned();
        (text, broken_sequences(bytes))
    };
    let out = super::convert(&text, from, Charset::Unicode);
    Conversion {
        unmapped: out.unmapped + damage,
        ..out
    }
}

/// How many characters lossy UTF-8 decoding had to replace. Zero for valid input,
/// and replacement characters the source genuinely encoded are discounted so a
/// document that legitimately contains one is not reported as damaged.
fn broken_sequences(bytes: &[u8]) -> usize {
    if std::str::from_utf8(bytes).is_ok() {
        return 0;
    }
    String::from_utf8_lossy(bytes)
        .matches(char::REPLACEMENT_CHARACTER)
        .count()
        .saturating_sub(genuine_replacements(bytes))
}

/// How many `U+FFFD` the bytes genuinely encode.
fn genuine_replacements(bytes: &[u8]) -> usize {
    bytes
        .windows(3)
        .filter(|window| *window == [0xEF, 0xBF, 0xBD])
        .count()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_pivot_encodes_every_atom_exactly() {
        let mut out = String::new();
        for c in ['a', 'ệ', 'Ự', 'đ', 'Đ', 'b', '₫'] {
            assert!(
                encode(Charset::Unicode, Atom::from_char(c), &mut out),
                "{c} should be exact in Unicode"
            );
        }
        assert_eq!(out, "aệỰđĐb₫");
    }

    #[test]
    fn only_the_legacy_charsets_are_byte_oriented() {
        assert!(!is_byte_oriented(Charset::Unicode));
        assert!(is_byte_oriented(Charset::Tcvn3));
    }
}
