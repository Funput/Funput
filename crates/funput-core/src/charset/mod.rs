//! Converting Vietnamese text between Unicode and the legacy encodings.
//!
//! Vietnamese government documents still circulate in TCVN3, drawn by the
//! `.VnTime` fonts. This turns them into Unicode, and back.
//!
//! # Text in, text out
//!
//! [`convert`] takes a `&str`, not bytes, and that is not a convenience — it is
//! what the input actually is. Word stores a TCVN3 document as Unicode code points
//! `U+0020..=U+00FF` and leaves the *font* to draw the Vietnamese glyphs, so text
//! arriving from the clipboard is already "bytes as chars". Converting it means
//! reading those code points back as TCVN3 codes. [`decode_bytes`] is the other
//! door, for text that really is bytes because it came from a file.
//!
//! # How it is put together
//!
//! Everything pivots through precomposed Unicode: `source → Atom → target`. A
//! charset therefore never has to know about any other charset, and adding one
//! costs two mappings rather than one per charset already present. See
//! [`codecs`](self::codecs) for what a new charset owes, and
//! `docs/features/charset.md` for the design in full.
//!
//! ```
//! use funput_core::charset::{Charset, convert};
//!
//! let out = convert("Việt Nam", Charset::Unicode, Charset::Tcvn3);
//! assert_eq!(convert(&out.text, Charset::Tcvn3, Charset::Unicode).text, "Việt Nam");
//! assert_eq!(out.unmapped, 0);
//! ```

mod codecs;
mod pivot;
mod transcode;

/// A Vietnamese character encoding.
///
/// `#[non_exhaustive]`: Unicode tổ hợp is still to come, and adding it should not
/// break callers. Match with a wildcard arm.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
#[non_exhaustive]
pub enum Charset {
    /// Precomposed Unicode (NFC) — the pivot, and what every modern system uses.
    Unicode,
    /// TCVN3, also called ABC: the `.VnTime` encoding of Vietnamese government
    /// documents. One byte per letter, and no code for an uppercase toned vowel.
    Tcvn3,
    /// VNI-Windows, the `VNI-Times` encoding. Spells most letters as a base byte
    /// plus a mark byte, so a letter is not a character and conversion moves
    /// character boundaries. Named for the encoding rather than `Vni`, which would
    /// sit confusingly beside [`crate::InputMethod::Vni`] — a way of *typing*, not
    /// a way of storing.
    VniWindows,
}

/// Converted text, and how many characters did not survive it exactly.
///
/// `unmapped` counts characters, not bytes, and never counts *lost* text:
/// conversion always writes something for every character, so the output holds as
/// much as the input did. What the number says is how many places need the user's
/// eye. A character lands there for one of two reasons:
///
/// - the **target** cannot spell it — a `₫`, which no legacy charset has a code
///   for, or an uppercase toned vowel, which is TCVN3's own gap rather than a
///   general one: VNI-Windows spells those perfectly well;
/// - the **source** never defined it, so reading it was a guess.
///
/// The second is worth watching. Text converted from the wrong source charset is
/// mostly unrecognized units, so a count near the length of the input is the
/// clearest signal there is that the user picked the wrong bảng mã.
#[derive(Debug, Clone, PartialEq, Eq)]
#[non_exhaustive]
pub struct Conversion {
    pub text: String,
    pub unmapped: usize,
}

/// Convert text from one charset to another.
///
/// `from` must be what the text actually is; guessing it is a separate job, and
/// not one this function does.
pub fn convert(text: &str, from: Charset, to: Charset) -> Conversion {
    transcode::transcode(text, from, to)
}

/// Read raw bytes in `from` and return them as Unicode.
///
/// For a legacy charset the bytes are read one-to-one as code points, which is
/// exactly how the document that produced them was stored. For [`Charset::Unicode`]
/// they are decoded as UTF-8, and any invalid sequence becomes `U+FFFD` and counts
/// toward `unmapped` — a caller reading a file it merely *believes* is UTF-8 gets
/// told when it was wrong instead of silently receiving replacement characters.
pub fn decode_bytes(bytes: &[u8], from: Charset) -> Conversion {
    if codecs::is_byte_oriented(from) {
        let text: String = bytes.iter().copied().map(char::from).collect();
        return convert(&text, from, Charset::Unicode);
    }
    let Err(_) = std::str::from_utf8(bytes) else {
        return Conversion {
            text: String::from_utf8_lossy(bytes).into_owned(),
            unmapped: 0,
        };
    };
    let text = String::from_utf8_lossy(bytes).into_owned();
    let unmapped = text
        .matches(char::REPLACEMENT_CHARACTER)
        .count()
        .saturating_sub(genuine_replacements(bytes));
    Conversion { text, unmapped }
}

/// How many `U+FFFD` the bytes genuinely encode. Subtracting these is what keeps a
/// source that legitimately contains a replacement character from being reported
/// as damaged.
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
    fn legacy_bytes_are_read_one_to_one_as_code_points() {
        // 0xD6 is `ệ`; the rest is ASCII. This is what a .VnTime file holds.
        let bytes = [0x56, 0x69, 0xD6, 0x74];
        let out = decode_bytes(&bytes, Charset::Tcvn3);
        assert_eq!(out.text, "Việt");
        assert_eq!(out.unmapped, 0);
    }

    #[test]
    fn utf8_bytes_are_decoded_as_utf8() {
        let out = decode_bytes("Việt".as_bytes(), Charset::Unicode);
        assert_eq!(out.text, "Việt");
        assert_eq!(out.unmapped, 0);
    }

    #[test]
    fn broken_utf8_is_reported_rather_than_silently_replaced() {
        let out = decode_bytes(&[0x56, 0xFF, 0x69], Charset::Unicode);
        assert_eq!(out.unmapped, 1);
    }

    #[test]
    fn a_replacement_character_in_the_source_is_not_counted_as_damage() {
        let out = decode_bytes("a\u{FFFD}b".as_bytes(), Charset::Unicode);
        assert_eq!(out.text, "a\u{FFFD}b");
        assert_eq!(out.unmapped, 0);
    }
}
