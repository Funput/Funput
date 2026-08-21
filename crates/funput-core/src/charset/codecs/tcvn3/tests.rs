use super::*;
use crate::charset::Charset;
use crate::charset::transcode::transcode;
use crate::unicode::vowels;

fn to_tcvn3(text: &str) -> (String, usize) {
    let out = transcode(text, Charset::Unicode, Charset::Tcvn3);
    (out.text, out.unmapped)
}

fn to_unicode(text: &str) -> String {
    transcode(text, Charset::Tcvn3, Charset::Unicode).text
}

/// The sweep: every letter the inventory has, encoded and read back.
///
/// The round trip alone would pass on a table with two codes swapped between
/// families, because the error cancels. What catches that is the second half —
/// the set of letters that lose something has to be *exactly* the uppercase toned
/// vowels, no more and no fewer.
#[test]
fn the_whole_inventory_round_trips_except_uppercase_tones() {
    let mut lossy = Vec::new();

    for family in 0..vowels::FAMILY_COUNT {
        for tone in 0..6 {
            for upper in [false, true] {
                let c = vowels::vowel_glyph(family, tone, upper).expect("in inventory");
                let (encoded, unmapped) = to_tcvn3(&c.to_string());

                if unmapped == 0 {
                    assert_eq!(to_unicode(&encoded), c.to_string(), "{c} should round trip");
                    continue;
                }
                lossy.push(c);
                let lower = vowels::vowel_glyph(family, tone, false).expect("in inventory");
                assert_eq!(
                    to_unicode(&encoded),
                    lower.to_string(),
                    "{c} has no code; it should come back as {lower}"
                );
            }
        }
    }

    let uppercase_toned: Vec<char> = (0..vowels::FAMILY_COUNT)
        .flat_map(|family| (1..6).map(move |tone| (family, tone)))
        .filter_map(|(family, tone)| vowels::vowel_glyph(family, tone, true))
        .collect();
    assert_eq!(
        lossy, uppercase_toned,
        "only the uppercase toned vowels may lose anything"
    );
}

/// `đ` and `Đ` both have codes, so unlike the vowels they keep their case.
#[test]
fn the_stroke_keeps_its_case_in_both_directions() {
    for c in ['đ', 'Đ'] {
        let (encoded, unmapped) = to_tcvn3(&c.to_string());
        assert_eq!(unmapped, 0, "{c} has a code of its own");
        assert_eq!(to_unicode(&encoded), c.to_string());
    }
}

/// A transposed code is invisible to the eye. Two structural facts make one
/// visible to the build instead: no code is shared, and every code sits in the
/// high range where a legacy font draws Vietnamese.
#[test]
fn every_code_is_distinct_and_in_the_vietnamese_range() {
    let codes: Vec<u8> = table::TCVN3_BYTES
        .iter()
        .flatten()
        .copied()
        .filter(|&code| code != 0)
        .chain(table::UPPER_BASES.iter().map(|&(_, code)| code))
        .chain([table::STROKE_LOWER, table::STROKE_UPPER])
        .collect();

    // 12 families x 5 tones, + 6 lowercase shaped bases, + their 6 uppercase, + đĐ.
    assert_eq!(codes.len(), 74);
    for code in &codes {
        assert!(
            (0xA1..=0xFE).contains(code),
            "{code:#04X} is outside the range TCVN3 uses"
        );
    }

    let mut sorted = codes.clone();
    sorted.sort_unstable();
    sorted.dedup();
    assert_eq!(sorted.len(), codes.len(), "two letters share one code");
}

/// TCVN3 is ASCII below 0x80, so anything that is not Vietnamese survives a
/// round trip untouched — including the ASCII vowels, which take the vowel path
/// rather than the passthrough one.
#[test]
fn ascii_survives_untouched() {
    let text = "Ho Chi Minh 1975 (bd) -- ok!";
    let (encoded, unmapped) = to_tcvn3(text);
    assert_eq!(unmapped, 0);
    assert_eq!(encoded, text);
    assert_eq!(to_unicode(&encoded), text);
}

/// A character TCVN3 has no code for is reported, but never dropped.
#[test]
fn a_character_outside_the_charset_is_reported_and_kept() {
    let (encoded, unmapped) = to_tcvn3("giá 5₫");
    assert_eq!(unmapped, 1, "only ₫ is outside TCVN3");
    assert!(
        encoded.ends_with('₫'),
        "it must still be there: {encoded:?}"
    );
}

/// A high byte TCVN3 does not assign comes through as itself rather than being
/// forced onto the nearest letter — and is reported, because the same code point
/// is a Latin-1 letter too and re-encoding it can land on a different TCVN3 code.
#[test]
fn an_unassigned_high_byte_passes_through_and_is_reported() {
    let unassigned = char::from(0xB0); // between the đ block and the `a` family
    let out = transcode(&unassigned.to_string(), Charset::Tcvn3, Charset::Unicode);
    assert_eq!(out.text, unassigned.to_string());
    assert_eq!(out.unmapped, 1);
}

/// The case the property test found. `0xC2` is not a TCVN3 code, but the code
/// point it passes through as is `Â`, which *is* Vietnamese and whose TCVN3 code
/// is `0xA2`. Round-tripping therefore moves the byte — silently, unless the
/// unrecognized reading is counted. It is.
#[test]
fn an_unassigned_byte_that_is_a_latin1_letter_is_reported_not_normalized_silently() {
    let out = transcode("\u{C2}", Charset::Tcvn3, Charset::Unicode);
    assert_eq!(out.text, "Â");
    assert_eq!(out.unmapped, 1, "the source never defined 0xC2");

    let back = to_tcvn3(&out.text);
    assert_eq!(back.0, "\u{A2}", "re-encoding lands on Â's real code");
    assert_eq!(back.1, 0, "and that spelling is exact");
}

/// The word this whole feature exists for, spelled the way a `.VnTime` document
/// spells it. Fixed bytes, so a table edit has to face a concrete example and not
/// just a property.
#[test]
fn a_real_word_encodes_to_the_bytes_a_vntime_document_holds() {
    let (encoded, unmapped) = to_tcvn3("Việt Nam");
    assert_eq!(unmapped, 0);
    assert_eq!(
        encoded.chars().map(|c| c as u32).collect::<Vec<_>>(),
        vec![
            0x56, // V
            0x69, // i
            0xD6, // ệ
            0x74, // t
            0x20, // space
            0x4E, // N
            0x61, // a
            0x6D, // m
        ]
    );
    assert_eq!(to_unicode(&encoded), "Việt Nam");
}

/// A `.VnTime` document stores one byte per character, so it cannot hold `ệ` at
/// all. Reading precomposed Unicode as TCVN3 must say so — the text comes through
/// intact, but the count is what tells a user they picked the wrong bảng mã, and
/// what lets `detect` tell the two apart. Without this the two charsets score
/// identically on ordinary Vietnamese and detection is impossible.
#[test]
fn a_character_no_byte_could_hold_is_reported() {
    let out = transcode("Việt Nam", Charset::Tcvn3, Charset::Unicode);
    assert_eq!(out.text, "Việt Nam", "nothing is lost");
    assert_eq!(out.unmapped, 1, "but `ệ` was never TCVN3");
}
