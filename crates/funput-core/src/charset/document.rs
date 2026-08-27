//! Reading a whole document's bytes: what characters are in it, and what charset
//! spells them.
//!
//! The output is a `String`, and that is the subject of this module. [`convert`]
//! takes text rather than bytes because that is what a legacy document *is*: code
//! points `U+0020..=U+00FF` standing in for bytes, which a `.VnTime` font draws as
//! Vietnamese. Getting a file to that form is the job here.
//!
//! # Why this is not `funput-config`'s cascade
//!
//! `funput-config` reads UniKey macro tables and asks a stricter question — it will
//! not reinterpret a file unless the reinterpretation explains **every** character,
//! because a table of three typographic shortcuts is too little evidence to overturn
//! a UTF-8 decode that already worked. A document is not that: a page of Vietnamese
//! that mostly reads as TCVN3 *is* TCVN3, and a stray `°` proves nothing. Same
//! structure, different confidence, so they stay apart on purpose.
//!
//! [`convert`]: super::convert

use super::{Charset, detect, detect_bytes, is_byte_oriented};

/// A document as characters, and the charset those characters spell.
#[non_exhaustive]
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Document {
    pub text: String,
    /// `None` only when the bytes are not UTF-8 **and** no byte-oriented charset
    /// explains them. A caller should ask the user rather than pick something.
    pub charset: Option<Charset>,
}

/// The bytes carry a UTF-16 byte-order mark but do not decode as UTF-16.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct TruncatedUtf16;

impl core::fmt::Display for TruncatedUtf16 {
    fn fmt(&self, f: &mut core::fmt::Formatter<'_>) -> core::fmt::Result {
        f.write_str("this is a truncated UTF-16 file")
    }
}

impl core::error::Error for TruncatedUtf16 {}

/// Read a document's bytes as characters, and work out what charset they are in.
///
/// Takes the bytes by value so the common path — no mark, valid UTF-8 — keeps the
/// allocation instead of copying a whole document.
pub fn read(bytes: Vec<u8>) -> Result<Document, TruncatedUtf16> {
    Ok(identify(marks(bytes)?))
}

/// Deal with the byte-order marks, so what comes out is either UTF-8 or legacy bytes.
///
/// UTF-16 is decided by its mark rather than guessed at: falling through would trade
/// a certainty for a wrong answer, since UTF-16 text is not valid UTF-8 and would be
/// read as a legacy charset and confidently mangled. A UTF-8 mark is only a hint —
/// editors write one whatever follows — so it is stripped and the rest judged on its
/// own merits.
fn marks(bytes: Vec<u8>) -> Result<Vec<u8>, TruncatedUtf16> {
    for (mark, unit) in [
        ([0xFF, 0xFE], u16::from_le_bytes as fn([u8; 2]) -> u16),
        ([0xFE, 0xFF], u16::from_be_bytes),
    ] {
        if let Some(rest) = bytes.strip_prefix(&mark) {
            return utf16(rest, unit)
                .map(String::into_bytes)
                .ok_or(TruncatedUtf16);
        }
    }
    Ok(match bytes.strip_prefix(&[0xEF, 0xBB, 0xBF]) {
        Some(rest) => rest.to_vec(),
        None => bytes,
    })
}

/// Turn the bytes into characters, and say what charset those characters are in.
///
/// **Two doors, because there are two questions.** Bytes that decode as UTF-8 are
/// judged as *text*, and the characters are the ones UTF-8 spelled. That covers a
/// TCVN3 document someone opened in Notepad and saved again — valid UTF-8 whose text
/// is still TCVN3, and the commonest way a legacy file arrives today. Reading those
/// same bytes one-to-one instead would give `Ã` and `–` where the document has `Ö`.
///
/// Bytes that do not decode were never Unicode. They are read one-to-one, which is
/// how the program that wrote them stored them, and a charset that is *not* stored
/// one byte per character cannot be the answer — accepting one would convert
/// replacement characters as though they were the document.
fn identify(bytes: Vec<u8>) -> Document {
    match String::from_utf8(bytes) {
        Ok(text) => {
            // Falling back to `Unicode` is not a guess. `detect` returning `None`
            // means no legacy charset explains this better than Unicode does — and
            // the bytes just decoded as UTF-8, so Unicode is what they are.
            let charset = detect(&text).or(Some(Charset::Unicode));
            Document { text, charset }
        }
        Err(err) => {
            let bytes = err.as_bytes();
            let charset = detect_bytes(bytes).filter(|&c| is_byte_oriented(c));
            let text = bytes.iter().copied().map(char::from).collect();
            Document { text, charset }
        }
    }
}

fn utf16(bytes: &[u8], unit: fn([u8; 2]) -> u16) -> Option<String> {
    if !bytes.len().is_multiple_of(2) {
        return None;
    }
    let units: Vec<u16> = bytes
        .chunks_exact(2)
        .map(|pair| unit([pair[0], pair[1]]))
        .collect();
    String::from_utf16(&units).ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn of(bytes: &[u8]) -> Document {
        read(bytes.to_vec()).expect("well-formed fixture")
    }

    /// The door that is easy to take by mistake. These bytes are valid UTF-8, so the
    /// characters are what UTF-8 spells — reading them one-to-one instead would give
    /// `Ã` where the document has `Ö`, and convert that.
    #[test]
    fn a_legacy_document_re_saved_as_utf8_is_read_as_text() {
        let doc = of("vi\u{D6}t nam h\u{B5} n\u{E9}i".as_bytes());
        assert_eq!(doc.charset, Some(Charset::Tcvn3));
        assert!(doc.text.starts_with("vi\u{D6}t"), "{:?}", doc.text);
    }

    /// The other door: a file that really is one byte per character.
    #[test]
    fn a_legacy_document_stored_as_bytes_is_read_one_to_one() {
        let doc = of(b"vi\xD6t nam h\xB5 n\xE9i");
        assert_eq!(doc.charset, Some(Charset::Tcvn3));
        assert_eq!(doc.text.chars().next(), Some('v'));
        assert!(doc.text.contains('\u{D6}'));
    }

    /// The two doors reach the same document from the two ways it can be stored, so
    /// converting either one gives the same Vietnamese back.
    #[test]
    fn both_doors_agree_on_what_the_document_says() {
        let as_text = of("vi\u{D6}t nam h\u{B5} n\u{E9}i".as_bytes());
        let as_bytes = of(b"vi\xD6t nam h\xB5 n\xE9i");
        assert_eq!(as_text, as_bytes);

        let charset = as_text.charset.expect("identified");
        let out = super::super::convert(&as_text.text, charset, Charset::Unicode);
        assert_eq!(out.text, "việt nam hà nội");
        assert_eq!(out.unmapped, 0);
    }

    /// Not a guess: the bytes decoded as UTF-8, so Unicode is what they are. Only the
    /// byte door is allowed to come back with nothing.
    #[test]
    fn text_with_nothing_to_decide_is_unicode_rather_than_undecided() {
        assert_eq!(of(b"hello world").charset, Some(Charset::Unicode));
    }

    #[test]
    fn bytes_nothing_explains_are_left_undecided() {
        let doc = of(b"the quick brown \xFF fox jumps over the lazy dog");
        assert_eq!(doc.charset, None);
    }

    /// A charset that is not stored one byte per character cannot explain bytes that
    /// already failed a UTF-8 decode; accepting one would convert `U+FFFD` as though it
    /// were the document.
    #[test]
    fn the_byte_door_never_answers_with_a_unicode_charset() {
        for fixture in [
            b"vi\xD6t nam h\xB5 n\xE9i".as_slice(),
            b"\xFF\x00\xFE".as_slice(),
        ] {
            let charset = of(fixture).charset;
            assert!(
                charset.is_none_or(is_byte_oriented) || std::str::from_utf8(fixture).is_ok(),
                "{charset:?}"
            );
        }
    }

    #[test]
    fn a_utf8_mark_is_stripped_rather_than_converted() {
        assert_eq!(of(b"\xEF\xBB\xBFvn").text, "vn");
    }

    /// A UTF-16 mark is a verdict, not a hint. Without this the bytes would fail the
    /// UTF-8 decode and be read as a legacy charset — confidently, and wrongly.
    #[test]
    fn utf16_is_decoded_from_its_mark_in_both_byte_orders() {
        assert_eq!(of(b"\xFF\xFEV\x00i\x00\xC7\x1Et\x00").text, "Việt");
        assert_eq!(of(b"\xFE\xFF\x00V\x00i\x1E\xC7\x00t").text, "Việt");
    }

    #[test]
    fn a_truncated_utf16_file_is_refused_rather_than_half_read() {
        assert_eq!(read(b"\xFF\xFEv".to_vec()), Err(TruncatedUtf16));
    }

    /// UniKey writes a UTF-16 file with a mark and treats the mark as content when it
    /// converts; Funput reads it as a mark. Pins that a mark never reaches the text.
    #[test]
    fn a_utf16_mark_is_never_part_of_the_document() {
        for doc in [
            of(b"\xFF\xFEV\x00i\x00\xC7\x1Et\x00"),
            of(b"\xEF\xBB\xBFVi\xE1\xBB\x87t"),
        ] {
            assert!(!doc.text.contains('\u{FEFF}'), "{:?}", doc.text);
        }
    }
}
