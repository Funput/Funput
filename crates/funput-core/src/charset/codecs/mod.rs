//! Where a charset says how it spells an [`Atom`] — the extension point.
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
//!   the charset does not define comes back as [`Atom::Other`] with `recognized`
//!   false, never as an error.
//! - `encode(Atom, &mut String) -> bool` — write the atom, returning `false` when
//!   what it wrote is not exact. It always writes *something*; a character never
//!   disappears in conversion.

mod tcvn3;

use super::Charset;
use super::pivot::Atom;
use super::transcode::{Cursor, Decoded};

/// Read one unit of `charset` at the cursor.
pub(super) fn decode(charset: Charset, cur: &Cursor<'_>) -> Decoded {
    match charset {
        // The pivot is already the atom's own spelling — nothing to undo, and no
        // char it fails to define.
        Charset::Unicode => Decoded {
            atom: Atom::from_char(cur.first()),
            consumed: 1,
            recognized: true,
        },
        Charset::Tcvn3 => tcvn3::decode(cur),
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
    }
}

/// Whether a charset spells text in single bytes drawn by a legacy font, so bytes
/// read from a file are Latin-1 rather than UTF-8. Used only by
/// [`crate::charset::decode_bytes`].
pub(super) fn is_byte_oriented(charset: Charset) -> bool {
    match charset {
        Charset::Unicode => false,
        Charset::Tcvn3 => true,
    }
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
