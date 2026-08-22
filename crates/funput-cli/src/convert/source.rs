//! Getting the input, and working out what charset spelled it.
//!
//! The output is a `String`, and that is the whole subject of this module.
//! `funput_core::charset::convert` takes text rather than bytes because that is what
//! the input actually is: a legacy document is stored as code points `U+0020..=U+00FF`
//! standing in for bytes, and converting it means reading those code points back as
//! charset codes. Getting a file to that form is the job here, and there are two ways
//! in depending on what the bytes turn out to be.

use std::io::Read;
use std::path::Path;

use funput_core::charset::{self, Charset};

use crate::cli::CliError;

/// The document as characters, and what charset those characters spell.
pub(super) struct Input {
    pub(super) text: String,
    /// `None` only when the bytes are not UTF-8 **and** no byte-oriented charset
    /// explains them. The command then asks for `--from` rather than guessing.
    pub(super) charset: Option<Charset>,
}

/// Read `path`, or standard input when it is `None`.
pub(super) fn read(path: Option<&Path>) -> Result<Input, CliError> {
    let bytes = match path {
        Some(path) => std::fs::read(path)
            .map_err(|e| CliError::Msg(format!("cannot read {}: {e}", path.display())))?,
        None => {
            let mut buffer = Vec::new();
            std::io::stdin().read_to_end(&mut buffer)?;
            buffer
        }
    };
    from_bytes(bytes)
}

/// Interpret bytes already in hand.
///
/// Split from [`read`] so that everything this module decides can be exercised
/// without a file or a pipe — the deciding is the part worth testing.
pub(super) fn from_bytes(bytes: Vec<u8>) -> Result<Input, CliError> {
    Ok(identify(marks(bytes)?))
}

/// Deal with the byte-order marks, so what comes out is either UTF-8 or legacy bytes.
///
/// UTF-16 is decided by its mark rather than guessed at: falling through would trade
/// a certainty for a wrong answer, since UTF-16 text is not valid UTF-8 and would be
/// read as a legacy charset and confidently mangled. A UTF-8 mark is only a hint —
/// editors write one whatever follows — so it is stripped and the rest judged on its
/// own merits.
fn marks(bytes: Vec<u8>) -> Result<Vec<u8>, CliError> {
    for (mark, unit) in [
        ([0xFF, 0xFE], u16::from_le_bytes as fn([u8; 2]) -> u16),
        ([0xFE, 0xFF], u16::from_be_bytes),
    ] {
        if let Some(rest) = bytes.strip_prefix(&mark) {
            return utf16(rest, unit)
                .map(String::into_bytes)
                .ok_or_else(|| CliError::Msg("this is a truncated UTF-16 file".into()));
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
fn identify(bytes: Vec<u8>) -> Input {
    match String::from_utf8(bytes) {
        Ok(text) => {
            // Falling back to `Unicode` is not a guess. `detect` returning `None`
            // means no legacy charset explains this better than Unicode does — and
            // the bytes just decoded as UTF-8, so Unicode is what they are.
            let charset = charset::detect(&text).or(Some(Charset::Unicode));
            Input { text, charset }
        }
        Err(err) => {
            let bytes = err.as_bytes();
            let charset = charset::detect_bytes(bytes).filter(|&c| charset::is_byte_oriented(c));
            let text = bytes.iter().copied().map(char::from).collect();
            Input { text, charset }
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

    fn of(bytes: &[u8]) -> Input {
        identify(marks(bytes.to_vec()).expect("well-formed fixture"))
    }

    /// The door that used to be taken by mistake. These bytes are valid UTF-8, so the
    /// characters are what UTF-8 spells — reading them one-to-one instead would give
    /// `Ã` where the document has `Ö`, and convert that.
    #[test]
    fn a_legacy_document_re_saved_as_utf8_is_read_as_text() {
        let input = of("vi\u{D6}t nam h\u{B5} n\u{E9}i".as_bytes());
        assert_eq!(input.charset, Some(Charset::Tcvn3));
        assert!(input.text.starts_with("vi\u{D6}t"), "{:?}", input.text);
    }

    /// The other door: a file that really is one byte per character.
    #[test]
    fn a_legacy_document_stored_as_bytes_is_read_one_to_one() {
        let input = of(b"vi\xD6t nam h\xB5 n\xE9i");
        assert_eq!(input.charset, Some(Charset::Tcvn3));
        assert_eq!(input.text.chars().next(), Some('v'));
        assert!(input.text.contains('\u{D6}'));
    }

    /// Not a guess: the bytes decoded as UTF-8, so Unicode is what they are. Only the
    /// byte door is allowed to come back with nothing.
    #[test]
    fn text_with_nothing_to_decide_is_unicode_rather_than_undecided() {
        assert_eq!(of(b"hello world").charset, Some(Charset::Unicode));
    }

    #[test]
    fn bytes_nothing_explains_are_left_undecided() {
        let input = of(b"the quick brown \xFF fox jumps over the lazy dog");
        assert_eq!(input.charset, None);
    }

    /// A charset that is not stored one byte per character cannot explain bytes that
    /// already failed a UTF-8 decode; accepting one would convert `U+FFFD` as
    /// though it were the document.
    #[test]
    fn the_byte_door_never_answers_with_a_unicode_charset() {
        for fixture in [
            b"vi\xD6t nam h\xB5 n\xE9i".as_slice(),
            b"\xFF\x00\xFE".as_slice(),
        ] {
            let charset = of(fixture).charset;
            assert!(
                charset.is_none_or(charset::is_byte_oriented)
                    || std::str::from_utf8(fixture).is_ok(),
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
        assert!(marks(b"\xFF\xFEv".to_vec()).is_err());
    }
}
