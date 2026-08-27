use super::table::{CASE_OFFSET, STROKE_LOWER, VNI_BYTES};
use crate::charset::Charset;
use crate::charset::transcode::transcode;
use crate::unicode::shapes::base_vowel;
use crate::unicode::vowels;

fn to_vni(text: &str) -> (String, usize) {
    let out = transcode(text, Charset::Unicode, Charset::VniWindows);
    (out.text, out.unmapped)
}

fn from_vni(text: &str) -> (String, usize) {
    let out = transcode(text, Charset::VniWindows, Charset::Unicode);
    (out.text, out.unmapped)
}

fn codepoints(text: &str) -> Vec<u32> {
    text.chars().map(|c| c as u32).collect()
}

/// The sweep. Unlike TCVN3, whose uppercase toned vowels have no code, VNI spells
/// the entire inventory — so the set that loses anything must be **empty**. That
/// emptiness is the strongest single check on the table: a byte that collides with
/// another cell shows up as a round-trip mismatch, and one that collides with
/// nothing shows up as a non-zero `unmapped`.
#[test]
fn the_whole_inventory_round_trips_with_nothing_lost() {
    for family in 0..vowels::FAMILY_COUNT {
        for tone in 0..6 {
            for upper in [false, true] {
                let c = vowels::vowel_glyph(family, tone, upper).expect("in inventory");
                let (encoded, unmapped) = to_vni(&c.to_string());
                assert_eq!(unmapped, 0, "{c} should be spellable");
                assert_eq!(from_vni(&encoded).0, c.to_string(), "{c} should round trip");
            }
        }
    }
}

#[test]
fn the_stroke_keeps_its_case_in_both_directions() {
    for c in ['đ', 'Đ'] {
        let (encoded, unmapped) = to_vni(&c.to_string());
        assert_eq!(unmapped, 0);
        assert_eq!(from_vni(&encoded).0, c.to_string());
    }
}

/// A transposed byte is invisible to the eye, so assert structure instead. All
/// seventy-three spellings distinct is what licenses the flat reverse scan in
/// `bytes::coords_for` — two cells sharing a spelling would make it return
/// whichever family came first.
#[test]
fn every_spelling_is_distinct() {
    let mut spellings: Vec<(u8, u8)> = VNI_BYTES.iter().flatten().copied().collect();
    spellings.push((STROKE_LOWER, 0));
    assert_eq!(spellings.len(), 73, "12 families x 6 slots, plus đ");

    let mut sorted = spellings.clone();
    sorted.sort_unstable();
    sorted.dedup();
    assert_eq!(
        sorted.len(),
        spellings.len(),
        "two letters share one spelling"
    );
}

/// The invariant that makes greedy two-char matching safe: a mark byte is never a
/// letter on its own, so a pair that matches could not have been two letters. The
/// cross-case halves matter too, since decoding folds before it looks up.
#[test]
fn no_base_byte_is_ever_a_mark_byte() {
    let bases: Vec<u8> = VNI_BYTES
        .iter()
        .flatten()
        .map(|&(base, _)| base)
        .chain([STROKE_LOWER])
        .collect();
    let marks: Vec<u8> = VNI_BYTES
        .iter()
        .flatten()
        .map(|&(_, mark)| mark)
        .filter(|&mark| mark != 0)
        .collect();

    for base in &bases {
        assert!(
            !marks.contains(base),
            "{base:#04X} is both a base and a mark"
        );
        let raised = base - CASE_OFFSET;
        assert!(
            !marks.contains(&raised),
            "{raised:#04X} collides across case"
        );
        assert!(
            !bases.contains(&raised),
            "{raised:#04X} is a base in both cases"
        );
    }
}

/// Half the charset is derived rather than transcribed, so prove the rule that
/// derives it. Sixty-seven spellings, and not one uppercase byte written by hand.
#[test]
fn uppercase_is_every_byte_minus_the_case_offset() {
    for family in 0..vowels::FAMILY_COUNT {
        for tone in 0..6 {
            let lower = vowels::vowel_glyph(family, tone, false).expect("in inventory");
            let upper = vowels::vowel_glyph(family, tone, true).expect("in inventory");
            let lowered = codepoints(&to_vni(&lower.to_string()).0);
            let raised = codepoints(&to_vni(&upper.to_string()).0);
            let expected: Vec<u32> = lowered.iter().map(|c| c - u32::from(CASE_OFFSET)).collect();
            assert_eq!(raised, expected, "{upper} against {lower}");
        }
    }
    assert_eq!(
        codepoints(&to_vni("Đ").0),
        vec![u32::from(STROKE_LOWER) - u32::from(CASE_OFFSET)]
    );
}

/// The bases in the table have to stay the ones the inventory names, or a family
/// would be spelled with another family's letter. Families 8 and 10 are the two
/// deliberate exceptions: VNI gives `ơ` and `ư` bytes of their own rather than
/// building them on `o` and `u`.
#[test]
fn every_ascii_base_matches_the_inventory() {
    for (family, row) in VNI_BYTES.iter().enumerate() {
        let stem = vowels::vowel_glyph(family, 0, false).expect("in inventory");
        let ascii = base_vowel(stem).expect("a vowel has a base");
        let (base, _) = row[0];
        if base < 0x80 {
            assert_eq!(char::from(base), ascii, "family {family} ({stem})");
        } else {
            assert!(
                matches!(family, 8 | 10),
                "family {family} ({stem}) has a non-ASCII base without being ơ or ư"
            );
        }
    }
}

/// A base at the end of the text is a letter, not a truncated pair. Guards the
/// sentinel trap: folding "no second char" to zero would match the one-byte
/// spellings and consume a character that is not there.
#[test]
fn a_base_at_the_end_of_the_text_is_a_letter_on_its_own() {
    for text in ["a", "\u{F4}", "\u{D4}", "\u{ED}", "\u{F1}"] {
        let (out, unmapped) = from_vni(text);
        assert_eq!(unmapped, 0, "{text:?}");
        assert_eq!(out.chars().count(), 1, "{text:?} is one letter");
    }
}

/// The two ways a second char can fail to be a mark byte, both of which a `0`
/// sentinel or an `as u8` truncation would swallow silently. No property test in
/// the suite generates either: one is outside Latin-1, the other truncates *into*
/// the sắc mark.
#[test]
fn a_char_outside_latin1_after_a_base_is_not_eaten() {
    for text in ["a\u{2192}", "a\u{2F9}"] {
        let (out, _) = from_vni(text);
        assert_eq!(out.chars().count(), 2, "{text:?} lost a character: {out:?}");
        assert_eq!(out.chars().next(), Some('a'));
    }
}

/// A mark with nothing to attach to is not a letter. It must take the `Other`
/// path, not `Atom::from_char` — `ù` is a Vietnamese letter, and reading it as one
/// would look confident when it is a guess.
#[test]
fn a_mark_with_no_base_passes_through_and_is_reported() {
    let (out, unmapped) = from_vni("\u{F9}");
    assert_eq!(out, "\u{F9}", "not read as a letter");
    assert_eq!(unmapped, 1);

    let (out, unmapped) = from_vni("b\u{F9}");
    assert_eq!(out, "b\u{F9}");
    assert_eq!(unmapped, 1);
}

/// The `i` family spells every tone in one byte, so `i` takes no marks at all. A
/// converter that writes `í` as `i` plus sắc is not producing VNI, and saying so
/// is what keeps one letter to one spelling.
#[test]
fn a_mark_on_a_base_that_cannot_take_it_is_reported() {
    for text in ["i\u{F9}", "\u{F4}\u{E1}"] {
        let (out, unmapped) = from_vni(text);
        assert_eq!(out.chars().count(), 2, "{text:?} should stay two things");
        assert_eq!(unmapped, 1, "{text:?}");
    }
}

/// VNI never writes a pair whose halves disagree on case, but sloppy converters
/// do. Read it as the letter, in the base's case, and count it.
#[test]
fn a_mixed_case_pair_reads_as_the_letter_and_is_reported() {
    for (text, expected) in [("a\u{D9}", "á"), ("A\u{F9}", "Á"), ("a\u{C1}", "ấ")] {
        let (out, unmapped) = from_vni(text);
        assert_eq!(out, expected, "{text:?}");
        assert_eq!(unmapped, 1, "{text:?} is malformed VNI");
    }
}

/// A byte VNI does not assign comes through as itself. `0xE7` sits in the gap
/// between the mark blocks.
#[test]
fn an_unassigned_high_byte_passes_through_and_is_reported() {
    let (out, unmapped) = from_vni("\u{E7}");
    assert_eq!(out, "\u{E7}");
    assert_eq!(unmapped, 1);
}

/// TCVN3's `0xC2` trap, with a twist it cannot show. `0xFD` is unassigned in VNI
/// and passes through as `ý`, which *is* Vietnamese — so re-encoding it spells it
/// in two bytes and the text gets **longer**. And `0xD0` is `Ð`, Latin capital
/// Eth: it looks like `Đ` but VNI's `Đ` is `0xD1`, so it must not be helpfully
/// corrected.
#[test]
fn an_unassigned_byte_that_is_a_vietnamese_letter_changes_length_when_re_encoded() {
    let (out, unmapped) = from_vni("\u{FD}");
    assert_eq!(out, "ý");
    assert_eq!(unmapped, 1, "the source never defined 0xFD");
    assert_eq!(
        codepoints(&to_vni(&out).0),
        vec![0x79, 0xF9],
        "one char to two"
    );

    let (out, unmapped) = from_vni("\u{D0}");
    assert_eq!(out, "\u{D0}", "Ð is not Đ");
    assert_eq!(unmapped, 1);
}

/// ASCII is untouched, and the ASCII vowels have to come back as bare ASCII rather
/// than gaining a mark — they take the vowel path, not the passthrough one.
#[test]
fn ascii_survives_untouched() {
    let text = "Ha Noi 1975 (bd) -- ok!";
    let (encoded, unmapped) = to_vni(text);
    assert_eq!(unmapped, 0);
    assert_eq!(encoded, text);
    assert_eq!(from_vni(&encoded).0, text);
}

/// Known and reported, rather than discovered in the field: a passthrough whose
/// code point is a mark byte fuses with the letter before it. TCVN3 cannot do this,
/// because nothing in it combines. `unmapped` is non-zero, so the module's "exact
/// means reversible" contract survives.
#[test]
fn a_non_ascii_passthrough_can_fuse_with_the_letter_before_it() {
    let (encoded, unmapped) = to_vni("a\u{F8}");
    assert_eq!(unmapped, 1, "ø has no VNI code");
    assert_eq!(from_vni(&encoded).0, "à", "the two fused on the way back");
}

/// Two real words with their bytes written out, so a table edit has to face a
/// concrete example and not only a property. Both change length, and `đường`
/// exercises the sequence that a non-greedy decoder breaks: `ư` alone, then `ơ`
/// carrying a mark.
#[test]
fn a_real_word_encodes_to_the_bytes_a_vni_times_document_holds() {
    let (encoded, unmapped) = to_vni("Việt Nam");
    assert_eq!(unmapped, 0);
    assert_eq!(
        codepoints(&encoded),
        vec![0x56, 0x69, 0x65, 0xE4, 0x74, 0x20, 0x4E, 0x61, 0x6D],
        "V i ê+nặng t _ N a m"
    );
    assert_eq!(from_vni(&encoded).0, "Việt Nam");

    let (encoded, unmapped) = to_vni("đường");
    assert_eq!(unmapped, 0);
    assert_eq!(
        codepoints(&encoded),
        vec![0xF1, 0xF6, 0xF4, 0xF8, 0x6E, 0x67],
        "đ ư ơ+huyền n g"
    );
    assert_eq!(from_vni(&encoded).0, "đường");
}

/// One letter, two characters — the property TCVN3 does not have, stated plainly.
#[test]
fn a_letter_can_be_two_characters() {
    assert_eq!(to_vni("Ề").0.chars().count(), 2);
    assert_eq!(to_vni("ạ").0.chars().count(), 2);
    assert_eq!(to_vni("í").0.chars().count(), 1, "the i family is one byte");
}

/// A `VNI-Times` document stores one byte per character, so it cannot hold `ệ`.
/// Reading precomposed Unicode as VNI reports it — see the twin test in the TCVN3
/// codec for why detection depends on this.
#[test]
fn a_character_no_byte_could_hold_is_reported() {
    let (out, unmapped) = from_vni("Việt Nam");
    assert_eq!(out, "Việt Nam", "nothing is lost");
    assert_eq!(unmapped, 1, "but `ệ` was never VNI");
}
