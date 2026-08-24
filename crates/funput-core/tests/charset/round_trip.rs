//! Fixed cases, through the public API only.

use funput_core::charset::{Charset, convert, decode_bytes};

use crate::cases::{EXACT, TCVN3_LOSSY, VNI_LOSSY};

/// Every fixture row, once per legacy charset: `(charset, Unicode, its spelling)`.
fn legacy_rows() -> impl Iterator<Item = (Charset, &'static str, &'static str)> {
    EXACT.iter().flat_map(|&(unicode, tcvn3, vni)| {
        [
            (Charset::Tcvn3, unicode, tcvn3),
            (Charset::VniWindows, unicode, vni),
        ]
    })
}

#[test]
fn unicode_encodes_to_the_bytes_a_legacy_document_holds() {
    for (charset, unicode, legacy) in legacy_rows() {
        let out = convert(unicode, Charset::Unicode, charset);
        assert_eq!(out.text, legacy, "{charset:?} encoding {unicode:?}");
        assert_eq!(out.unmapped, 0, "{charset:?} encoding {unicode:?}");
    }
}

#[test]
fn a_legacy_document_decodes_back_to_unicode() {
    for (charset, unicode, legacy) in legacy_rows() {
        let out = convert(legacy, charset, Charset::Unicode);
        assert_eq!(out.text, unicode, "{charset:?} decoding {legacy:?}");
        assert_eq!(out.unmapped, 0, "{charset:?} decoding {legacy:?}");
    }
}

/// The claim the pivot exists to make: `TCVN3 ↔ VNI` works without either codec
/// knowing the other exists. Nobody wrote a line of code for this pair.
#[test]
fn converting_between_two_legacy_charsets_needs_no_code_of_its_own() {
    for row in EXACT {
        let (unicode, tcvn3, vni) = *row;

        let out = convert(tcvn3, Charset::Tcvn3, Charset::VniWindows);
        assert_eq!(out.text, vni, "TCVN3 to VNI for {unicode:?}");
        assert_eq!(out.unmapped, 0);

        let out = convert(vni, Charset::VniWindows, Charset::Tcvn3);
        assert_eq!(out.text, tcvn3, "VNI to TCVN3 for {unicode:?}");
        assert_eq!(out.unmapped, 0);
    }
}

/// The two charsets lose different things, and the loss lands wherever the gap is.
/// `ĐIỀU 5` survives VNI intact and only degrades once it reaches TCVN3.
#[test]
fn loss_appears_only_at_the_end_that_cannot_spell_it() {
    let vni = convert("ĐIỀU 5", Charset::Unicode, Charset::VniWindows);
    assert_eq!(vni.unmapped, 0, "VNI spells uppercase toned vowels");

    let tcvn3 = convert(&vni.text, Charset::VniWindows, Charset::Tcvn3);
    assert_eq!(tcvn3.unmapped, 1, "TCVN3 is where Ề runs out of codes");
    assert_eq!(
        convert(&tcvn3.text, Charset::Tcvn3, Charset::Unicode).text,
        "ĐIềU 5"
    );
}

#[test]
fn what_a_charset_cannot_spell_is_reported_and_approximated() {
    for (charset, rows) in [
        (Charset::Tcvn3, TCVN3_LOSSY),
        (Charset::VniWindows, VNI_LOSSY),
    ] {
        for (unicode, legacy, unmapped, reread) in rows {
            let out = convert(unicode, Charset::Unicode, charset);
            assert_eq!(&out.text, legacy, "{charset:?} encoding {unicode:?}");
            assert_eq!(out.unmapped, *unmapped, "{charset:?} encoding {unicode:?}");

            let back = convert(&out.text, charset, Charset::Unicode);
            assert_eq!(&back.text, reread, "{charset:?} reading {unicode:?} back");
        }
    }
}

/// Converting to the charset the text is already in changes nothing. Worth its own
/// test because the UI will do exactly this whenever a user picks the same charset
/// on both sides.
#[test]
fn converting_a_charset_to_itself_is_an_identity() {
    for row in EXACT {
        let pairs = [
            (row.0, Charset::Unicode),
            (row.1, Charset::Tcvn3),
            (row.2, Charset::VniWindows),
        ];
        for (text, charset) in pairs {
            let out = convert(text, charset, charset);
            assert_eq!(out.text, text, "{text:?} through {charset:?}");
            assert_eq!(out.unmapped, 0);
        }
    }
}

/// The file door. A legacy file's bytes are the code points the clipboard would
/// have carried, so both doors have to land on the same text.
#[test]
fn reading_a_file_agrees_with_reading_the_clipboard() {
    for (charset, unicode, legacy) in legacy_rows() {
        let bytes: Vec<u8> = legacy.chars().map(|c| c as u8).collect();
        let out = decode_bytes(&bytes, charset);
        assert_eq!(out.text, unicode, "{charset:?} reading {legacy:?}");
        assert_eq!(out.unmapped, 0);
    }
}
