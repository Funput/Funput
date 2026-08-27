use funput_core::charset::document;
use funput_core::charset::{self, Charset};

use super::*;

/// Everything `run` does between reading and writing, with the I/O left out.
fn pipeline(bytes: &[u8], to: Charset) -> (Vec<u8>, usize, usize) {
    let doc = document::read(bytes.to_vec()).expect("well-formed fixture");
    let from = doc.charset.expect("fixture should be identifiable");
    let rendered = charset::render(&charset::read(&doc.text, from), to);
    (
        rendered.bytes,
        rendered.cost.undefined,
        rendered.cost.unrepresentable,
    )
}

#[test]
fn every_slug_in_the_list_resolves_and_nothing_else_does() {
    for charset in charset::ALL {
        assert_eq!(by_slug(charset.slug()).ok(), Some(charset));
    }
    assert!(by_slug("viscii").is_err());
    assert!(by_slug("").is_err());
}

/// The error is what the user reads after a typo, so it has to say what would have
/// worked — and it is built from `ALL`, so a charset added to core joins it.
#[test]
fn an_unknown_slug_is_answered_with_the_ones_that_work() {
    let message = report::unknown_slug("tcvn");
    assert!(message.contains("tcvn"));
    for charset in charset::ALL {
        assert!(message.contains(charset.slug()), "{message}");
    }
}

/// The feature, end to end: a `.VnTime` document becomes Unicode.
#[test]
fn a_legacy_document_converts_to_unicode() {
    let (bytes, read, written) = pipeline(b"vi\xD6t nam h\xB5 n\xE9i", Charset::Unicode);
    assert_eq!(String::from_utf8(bytes).unwrap(), "việt nam hà nội");
    assert_eq!((read, written), (0, 0));
}

/// And back the other way, as the bytes a `.VnTime` document holds — one per letter,
/// not the two UTF-8 would spend on each.
#[test]
fn converting_to_a_legacy_charset_writes_one_byte_per_letter() {
    let (bytes, _, written) = pipeline("Việt".as_bytes(), Charset::Tcvn3);
    assert_eq!(bytes, b"Vi\xD6t");
    assert_eq!(written, 0);
}

#[test]
fn text_survives_a_trip_out_to_every_charset_and_back() {
    for charset in charset::ALL {
        let (there, ..) = pipeline("Việt Nam, Hà Nội".as_bytes(), charset);
        let (back, ..) = pipeline(&there, Charset::Unicode);
        assert_eq!(
            String::from_utf8(back).unwrap(),
            "Việt Nam, Hà Nội",
            "{}",
            charset.slug()
        );
    }
}

/// TCVN3 has no code for an uppercase toned vowel and none for `₫`. Neither is
/// dropped — the document is still written — but the count is what the warning is
/// built from.
#[test]
fn what_the_target_cannot_represent_is_counted_rather_than_dropped() {
    let (bytes, read, written) = pipeline("Ề 5₫".as_bytes(), Charset::Tcvn3);
    assert_eq!(written, 2);
    assert_eq!(
        read, 0,
        "the source is fine; it is the target that cannot cope"
    );
    assert!(!bytes.is_empty());
}
