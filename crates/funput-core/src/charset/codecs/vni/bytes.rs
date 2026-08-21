//! Byte-level queries over [`super::table`] — case folding and the two lookups.
//!
//! Everything here is a scan over seventy-two cells. `unicode::vowels` builds a
//! dense table at compile time because it runs once per keystroke; conversion runs
//! once per paste, where a scan costs nothing and keeps the data in one readable
//! block next to its only reader.

use super::table::{CASE_OFFSET, Cell, STROKE_LOWER, VNI_BYTES};

/// Whether `byte` appears in the table as a base, as a mark, or as `đ`.
fn is_lowercase_byte(byte: u8) -> bool {
    byte == STROKE_LOWER
        || VNI_BYTES
            .iter()
            .flatten()
            .any(|&(base, mark)| base == byte || (mark != 0 && mark == byte))
}

/// Fold a byte to its lowercase table form, reporting whether it was uppercase.
///
/// Unambiguous because the lowercase bytes (`0x61 0x65 0x69 0x6F 0x75 0x79` and
/// `0xE0..=0xFC`) and their uppercase counterparts (`0x41…` and `0xC0..=0xDC`)
/// share no value — so testing lowercase first can never mistake one for the
/// other. `None` for anything the charset does not use.
pub(super) fn fold(byte: u8) -> Option<(u8, bool)> {
    if is_lowercase_byte(byte) {
        return Some((byte, false));
    }
    let lowered = byte.checked_add(CASE_OFFSET)?;
    is_lowercase_byte(lowered).then_some((lowered, true))
}

/// A lowercase table byte written in the requested case.
pub(super) fn cased(byte: u8, upper: bool) -> char {
    char::from(if upper { byte - CASE_OFFSET } else { byte })
}

/// The inventory coordinates spelled by `base` plus `mark`, where `mark` is `0`
/// for a one-byte letter.
///
/// A flat scan is sound here only because the seventy-two cells are pairwise
/// distinct — `tests.rs` asserts that. TCVN3's reverse lookup searches for a
/// single byte and would find the *first* family sharing it; the same shape over
/// this table would read `ó` as `á`, since both spell their sắc with `0xF9`.
pub(super) fn coords_for(base: u8, mark: u8) -> Option<(usize, usize)> {
    VNI_BYTES.iter().enumerate().find_map(|(family, row)| {
        row.iter()
            .position(|&cell| cell == (base, mark))
            .map(|tone| (family, tone))
    })
}

/// How VNI spells the letter at these coordinates.
pub(super) fn spelling(family: usize, tone: usize) -> Cell {
    VNI_BYTES[family][tone]
}

/// The byte a char stands for in a legacy document, or `None` when the char is
/// outside Latin-1 and so cannot be one.
///
/// `u8::try_from`, never `as u8`: truncation would read `U+02F9` as the sắc mark
/// and quietly eat a character that was never VNI at all.
pub(super) fn byte_of(c: char) -> Option<u8> {
    u8::try_from(c as u32).ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn folding_recovers_the_case_of_every_table_byte() {
        for &(base, mark) in VNI_BYTES.iter().flatten() {
            assert_eq!(fold(base), Some((base, false)));
            assert_eq!(fold(base - CASE_OFFSET), Some((base, true)));
            if mark != 0 {
                assert_eq!(fold(mark), Some((mark, false)));
                assert_eq!(fold(mark - CASE_OFFSET), Some((mark, true)));
            }
        }
        assert_eq!(fold(STROKE_LOWER), Some((STROKE_LOWER, false)));
    }

    #[test]
    fn folding_refuses_bytes_the_charset_never_uses() {
        // 0xE7 and 0xF0 sit in the gaps between the mark blocks; 'b' and '5' are
        // ASCII that is not a vowel base.
        for byte in [0xE7, 0xF0, 0xF7, b'b', b'5', b' '] {
            assert_eq!(fold(byte), None, "{byte:#04X}");
        }
    }

    #[test]
    fn folding_an_uppercase_byte_near_the_top_does_not_overflow() {
        assert_eq!(fold(0xFF), None);
    }

    #[test]
    fn a_char_outside_latin1_has_no_byte() {
        assert_eq!(byte_of('→'), None);
        assert_eq!(
            byte_of('\u{2F9}'),
            None,
            "must not truncate to the sắc mark"
        );
        assert_eq!(byte_of('\u{F9}'), Some(0xF9));
    }
}
