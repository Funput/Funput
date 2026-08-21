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
mod detect;
mod pivot;
mod transcode;

pub use detect::{detect, detect_bytes};

/// A Vietnamese character encoding.
///
/// `#[non_exhaustive]`: VIQR, VISCII and the rest are out of scope today but not
/// forever, and adding one should not break callers. Match with a wildcard arm.
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
    /// Unicode tổ hợp, UniKey's convention: the shaped vowel stays precomposed and
    /// only the **tone** rides as a combining mark, so `ậ` is `â` plus `U+0323`.
    ///
    /// Deliberately not called `Decomposed`: this is not NFD, which would take the
    /// shape apart too. Reading accepts NFD anyway — see the codec — but writing
    /// only ever produces this form.
    UnicodeCombining,
}

/// Converted text, and what happened to it on the way.
///
/// Both counts are in characters, and neither counts *lost* text: conversion
/// always writes something for every character, so the output holds as much as the
/// input did. A character lands in at most one of them.
///
/// **`unmapped`** — something was lost or guessed, so the output is not certainly
/// equivalent to the input. Either the target cannot spell it (a `₫`, which no
/// legacy charset has a code for; an uppercase toned vowel, which is TCVN3's own
/// gap rather than a general one) or the source never defined it, making the
/// reading a guess. Text read with the wrong source charset is mostly unrecognized
/// units, so a count near the length of the input is the clearest signal there is
/// that the user picked the wrong bảng mã.
///
/// **`normalized`** — understood beyond doubt, nothing lost, only the spelling
/// changed. Reading NFC text as Unicode tổ hợp lands here: every letter comes
/// through intact, just written the charset's own way. Keeping it out of
/// `unmapped` is what lets an interface say "nothing was lost" and mean it.
#[derive(Debug, Clone, PartialEq, Eq)]
#[non_exhaustive]
pub struct Conversion {
    pub text: String,
    pub unmapped: usize,
    pub normalized: usize,
}

/// Convert text from one charset to another.
///
/// `from` must be what the text actually is; guessing it is [`detect`]'s job, and
/// not one this function does.
pub fn convert(text: &str, from: Charset, to: Charset) -> Conversion {
    transcode::transcode(text, from, to)
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
    let (text, damage) = if codecs::is_byte_oriented(from) {
        // Latin-1 is total, so reading bytes as code points cannot fail.
        (bytes.iter().copied().map(char::from).collect(), 0)
    } else {
        let text = String::from_utf8_lossy(bytes).into_owned();
        (text, codecs::broken_sequences(bytes))
    };
    let out = convert(&text, from, Charset::Unicode);
    Conversion {
        unmapped: out.unmapped + damage,
        ..out
    }
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
