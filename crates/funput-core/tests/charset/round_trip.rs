//! Fixed cases, through the public API only.

use funput_core::charset::{Charset, convert, decode_bytes};

use crate::cases::{EXACT, LOSSY};

#[test]
fn unicode_encodes_to_the_bytes_a_vntime_document_holds() {
    for (unicode, tcvn3) in EXACT {
        let out = convert(unicode, Charset::Unicode, Charset::Tcvn3);
        assert_eq!(&out.text, tcvn3, "encoding {unicode:?}");
        assert_eq!(out.unmapped, 0, "encoding {unicode:?}");
    }
}

#[test]
fn a_vntime_document_decodes_back_to_unicode() {
    for (unicode, tcvn3) in EXACT {
        let out = convert(tcvn3, Charset::Tcvn3, Charset::Unicode);
        assert_eq!(&out.text, unicode, "decoding {tcvn3:?}");
        assert_eq!(out.unmapped, 0, "decoding {tcvn3:?}");
    }
}

/// What TCVN3 cannot spell is reported and approximated, never dropped — and the
/// approximation is the one the module documents, not whatever fell out.
#[test]
fn the_two_kinds_of_loss_are_reported_and_approximated() {
    for (unicode, tcvn3, unmapped, reread) in LOSSY {
        let out = convert(unicode, Charset::Unicode, Charset::Tcvn3);
        assert_eq!(&out.text, tcvn3, "encoding {unicode:?}");
        assert_eq!(out.unmapped, *unmapped, "encoding {unicode:?}");

        let back = convert(&out.text, Charset::Tcvn3, Charset::Unicode);
        assert_eq!(&back.text, reread, "reading {unicode:?} back");
    }
}

/// Converting to the charset the text is already in changes nothing. Worth its own
/// test because the UI will do exactly this whenever a user picks the same charset
/// on both sides.
#[test]
fn converting_a_charset_to_itself_is_an_identity() {
    for (unicode, tcvn3) in EXACT {
        for (text, charset) in [(unicode, Charset::Unicode), (tcvn3, Charset::Tcvn3)] {
            let out = convert(text, charset, charset);
            assert_eq!(&out.text, text, "{text:?} through {charset:?}");
            assert_eq!(out.unmapped, 0);
        }
    }
}

/// The file door. A `.VnTime` file's bytes are the code points the clipboard would
/// have carried, so both doors have to land on the same text.
#[test]
fn reading_a_file_agrees_with_reading_the_clipboard() {
    for (unicode, tcvn3) in EXACT {
        let bytes: Vec<u8> = tcvn3.chars().map(|c| c as u8).collect();
        let out = decode_bytes(&bytes, Charset::Tcvn3);
        assert_eq!(&out.text, unicode, "reading {tcvn3:?} as bytes");
        assert_eq!(out.unmapped, 0);
    }
}
