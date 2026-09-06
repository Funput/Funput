//! Turning a macro file's bytes into text, whatever charset wrote it.
//!
//! Two questions, in order. **How are the bytes stored** — UTF-8, UTF-16, or one
//! byte per character? Then **which Vietnamese charset spells the text** — Unicode,
//! or one of the legacy ones a `.VnTime` or `VNI-Times` document uses.
//!
//! The second question is [`funput_core::charset`]'s, and asking it is what lets a
//! UniKey table written before Unicode import at all. It used to be refused, with a
//! message telling the user to go re-export from UniKey.
//!
//! # Guessing, carefully
//!
//! The old refusal had a good reason: guessing wrong "would silently import mojibake
//! the user then has to hunt down row by row". Three things keep that from happening
//! now. `detect` declines rather than picks when the evidence does not separate the
//! charsets. Bytes that failed a UTF-8 decode may only be answered with a
//! byte-oriented charset, or `decode_bytes` would hand back replacement characters
//! dressed up as a reading. And whatever is guessed comes back to the caller, so the
//! user is told rather than left to find out.

use funput_core::charset::{self, Charset};

/// Text from a macro file, and the charset it turned out to be written in.
///
/// `None` means nothing needed deciding — the file is ASCII, or carries too little
/// Vietnamese to judge. Reporting `Unicode` there would invent a certainty nobody
/// has.
pub(super) struct Decoded {
    pub(super) text: String,
    pub(super) charset: Option<Charset>,
}

/// Read a macro file's bytes as text. `None` when nothing explains them.
pub(super) fn decode(bytes: &[u8]) -> Option<Decoded> {
    // A UTF-16 byte-order mark is unambiguous. Falling through to detection here
    // would trade a precise diagnosis for a guess at Latin-1 nonsense.
    if let Some(rest) = bytes.strip_prefix(&[0xFF, 0xFE]) {
        return utf16(rest, u16::from_le_bytes).map(unicode_text);
    }
    if let Some(rest) = bytes.strip_prefix(&[0xFE, 0xFF]) {
        return utf16(rest, u16::from_be_bytes).map(unicode_text);
    }
    // A UTF-8 BOM is a hint, not a verdict: editors write one regardless of what
    // follows. Strip it and judge the rest — including falling through to the byte
    // door, which is why the stripped bytes are what carries on.
    let body = bytes.strip_prefix(&[0xEF, 0xBB, 0xBF]).unwrap_or(bytes);
    match std::str::from_utf8(body) {
        Ok(text) => Some(unicode_text(text.to_string())),
        Err(_) => from_legacy_bytes(body),
    }
}

/// Text that is already valid Unicode, normalised if it turns out to be spelled in
/// some other Vietnamese charset.
///
/// Two of those reach here. A file in Unicode tổ hợp is valid UTF-8 already. So is a
/// TCVN3 file that someone opened in Notepad and saved again — cp1252 and Latin-1
/// agree across the whole range TCVN3 uses, so every code point survives and the
/// result is valid UTF-8 whose *text* is still TCVN3. Both used to import as
/// nonsense, silently.
fn unicode_text(text: String) -> Decoded {
    let sample = sample(text.as_bytes());
    // Cutting at ASCII bytes of valid UTF-8 always lands on a character boundary,
    // so the sample is valid UTF-8 too.
    let sample = std::str::from_utf8(&sample).unwrap_or(&text);
    let detected = charset::detect(sample);
    let Some(other) = detected.filter(|&c| c != Charset::Unicode) else {
        return Decoded {
            text,
            charset: detected,
        };
    };

    // Overriding a UTF-8 decode that already worked takes more than a majority
    // vote: the reinterpretation has to explain **everything**.
    //
    // What this refuses is a table of typographic shortcuts. `µ` is TCVN3's `à` and
    // `¶` is its `ả`, so two values in three read as Vietnamese and the vote
    // carries — but `°` has no TCVN3 code at all, and that leftover is the tell.
    // Without this, `mu:µ` imports as `mu:à`.
    let converted = charset::convert(&text, other, Charset::Unicode);
    if converted.unmapped > 0 {
        return Decoded {
            text,
            charset: Some(Charset::Unicode),
        };
    }
    Decoded {
        text: converted.text,
        charset: Some(other),
    }
}

/// Bytes that are not UTF-8 at all: one byte per character, if any charset says so.
fn from_legacy_bytes(bytes: &[u8]) -> Option<Decoded> {
    let charset = charset::detect_bytes(&sample(bytes))?;
    // These bytes already failed a UTF-8 decode, so a charset that is not stored one
    // byte per character cannot be the answer. Accepting one would run them through
    // lossy decoding and import `U+FFFD` as though it were text.
    if !charset::is_byte_oriented(charset) {
        return None;
    }
    Some(Decoded {
        text: charset::decode_bytes(bytes, charset).text,
        charset: Some(charset),
    })
}

/// The part of a macro file worth judging: everything to the right of the first
/// colon on each line.
///
/// A trigger is a key, not prose — `vn`, `ct`, `sdt`, `url`. None of them is a
/// Vietnamese syllable, so counting them drags every candidate down alike and a
/// table mixing Vietnamese with phone numbers and URLs stops being detectable at
/// all. Judging the values is not a workaround; it is the right question.
///
/// The split works *before* the charset is known because `:` and `\n` are ASCII in
/// every charset here, and cutting at an ASCII byte always lands on a character
/// boundary — so one function over bytes serves both doors.
fn sample(bytes: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(bytes.len());
    for line in bytes.split(|&b| b == b'\n') {
        if line.first() == Some(&b';') {
            continue;
        }
        let Some(colon) = line.iter().position(|&b| b == b':') else {
            continue;
        };
        out.extend_from_slice(&line[colon + 1..]);
        out.push(b'\n');
    }
    out
}

fn utf16(bytes: &[u8], unit: fn([u8; 2]) -> u16) -> Option<String> {
    if !bytes.len().is_multiple_of(2) {
        return None;
    }
    let units: Vec<u16> = bytes
        .as_chunks::<2>()
        .0
        .iter()
        .map(|pair| unit([pair[0], pair[1]]))
        .collect();
    String::from_utf16(&units).ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn text_of(bytes: &[u8]) -> Option<String> {
        decode(bytes).map(|d| d.text)
    }

    fn charset_of(bytes: &[u8]) -> Option<Charset> {
        decode(bytes).and_then(|d| d.charset)
    }

    #[test]
    fn a_utf8_bom_is_stripped_rather_than_decoded_into_the_text() {
        assert_eq!(text_of(b"\xEF\xBB\xBFvn").as_deref(), Some("vn"));
    }

    #[test]
    fn plain_utf8_without_a_bom_still_decodes() {
        assert_eq!(
            text_of("vn:việt nam".as_bytes()).as_deref(),
            Some("vn:việt nam")
        );
    }

    #[test]
    fn both_utf16_byte_orders_decode() {
        let text = "vn:việt nam";
        let mut le = vec![0xFF, 0xFE];
        let mut be = vec![0xFE, 0xFF];
        for unit in text.encode_utf16() {
            le.extend_from_slice(&unit.to_le_bytes());
            be.extend_from_slice(&unit.to_be_bytes());
        }
        assert_eq!(text_of(&le).as_deref(), Some(text));
        assert_eq!(text_of(&be).as_deref(), Some(text));
    }

    /// A UTF-16 byte-order mark is a verdict, not a hint: a truncated file gets a
    /// precise refusal rather than falling through to be guessed at as Latin-1.
    #[test]
    fn a_truncated_utf16_file_is_refused_rather_than_half_read() {
        assert!(decode(&[0xFF, 0xFE, 0x76]).is_none());
    }

    /// The feature: a legacy file used to be refused, and now reads.
    #[test]
    fn a_legacy_eight_bit_file_is_read_and_named() {
        let file = b"vn:vi\xD6t nam\r\nhn:h\xB5 n\xE9i\r\n";
        assert_eq!(charset_of(file), Some(Charset::Tcvn3));
        assert!(text_of(file).unwrap().contains("việt nam"));
    }

    /// Bytes no charset explains are still refused — the old promise, kept.
    #[test]
    fn bytes_nothing_explains_are_still_refused() {
        assert!(decode(b"note:the quick brown fox \xFF jumps over the lazy dog").is_none());
    }

    /// A file that needs no deciding says so, rather than claiming Unicode.
    #[test]
    fn an_ascii_file_names_no_charset() {
        assert_eq!(charset_of(b"vn:viet nam\nhn:ha noi\n"), None);
    }

    /// Only the values are judged. A table whose triggers outnumber its Vietnamese
    /// would otherwise drag every candidate below the detector's majority rule.
    #[test]
    fn only_the_value_side_of_a_line_is_judged() {
        assert_eq!(
            sample(b";comment\nvn:vi\xD6t nam\nno-colon\n"),
            b"vi\xD6t nam\n"
        );
    }
}
