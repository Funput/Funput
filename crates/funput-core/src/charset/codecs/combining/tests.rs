use crate::charset::Charset;
use crate::charset::transcode::transcode;
use crate::unicode::vowels;

/// `(text, unmapped, normalized)`.
fn to_combining(text: &str) -> (String, usize, usize) {
    let out = transcode(text, Charset::Unicode, Charset::UnicodeCombining);
    (out.text, out.unmapped, out.normalized)
}

fn from_combining(text: &str) -> (String, usize, usize) {
    let out = transcode(text, Charset::UnicodeCombining, Charset::Unicode);
    (out.text, out.unmapped, out.normalized)
}

fn points(text: &str) -> Vec<u32> {
    text.chars().map(|c| c as u32).collect()
}

/// **The reason `compose.rs` is not a fold.**
///
/// Canonical NFD puts the tone first whenever it is nặng — `U+0323` has a lower
/// combining class than the circumflex and the breve. Applying the marks one at a
/// time to a `char` would strip the tone when the shape arrives and hand back `â`.
/// These four letters are `Việt`, `một`, `cộng`, `nặng`.
#[test]
fn the_tone_first_nfd_order_keeps_its_tone() {
    for (nfd, expected) in [
        ("a\u{323}\u{302}", "ậ"),
        ("a\u{323}\u{306}", "ặ"),
        ("e\u{323}\u{302}", "ệ"),
        ("o\u{323}\u{302}", "ộ"),
        ("A\u{323}\u{302}", "Ậ"),
        ("O\u{323}\u{302}", "Ộ"),
    ] {
        let (out, unmapped, normalized) = from_combining(nfd);
        assert_eq!(out, expected, "reading {:?}", points(nfd));
        assert_eq!(unmapped, 0, "nothing is lost, only rewritten");
        assert_eq!(normalized, 1);
    }
}

/// The sweep. This charset is Unicode, so nothing in the inventory is unspellable
/// — the loss set is empty, as with VNI and unlike TCVN3.
#[test]
fn the_whole_inventory_round_trips_with_nothing_lost() {
    for family in 0..vowels::FAMILY_COUNT {
        for tone in 0..6 {
            for upper in [false, true] {
                let c = vowels::vowel_glyph(family, tone, upper).expect("in inventory");
                let (encoded, unmapped, normalized) = to_combining(&c.to_string());
                assert_eq!((unmapped, normalized), (0, 0), "{c} should be spellable");
                assert_eq!(from_combining(&encoded).0, c.to_string(), "{c}");
                assert_eq!(from_combining(&encoded).1, 0, "{c} reads back exactly");
                assert_eq!(from_combining(&encoded).2, 0, "{c} reads back canonically");
            }
        }
    }
}

/// Three spellings of the same letter all land on it. Only the middle one is what
/// this charset writes, and the sweep above already proves that.
#[test]
fn every_spelling_of_a_letter_decodes_to_the_same_letter() {
    for (nfd, unikey, precomposed) in [
        ("a\u{302}\u{301}", "â\u{301}", "ấ"),
        ("a\u{306}\u{301}", "ă\u{301}", "ắ"),
        ("o\u{31b}\u{323}", "ơ\u{323}", "ợ"),
        ("e\u{302}\u{300}", "ê\u{300}", "ề"),
        ("u\u{31b}\u{309}", "ư\u{309}", "ử"),
    ] {
        for spelling in [nfd, unikey, precomposed] {
            assert_eq!(from_combining(spelling).0, precomposed, "{spelling:?}");
        }
    }
}

/// A spelling this charset does not use is rewritten, not lost — that distinction
/// is the whole reason `Conversion` carries two counts. Full NFD decomposes the
/// shape; a precomposed letter keeps the tone attached. Neither loses anything.
#[test]
fn a_non_canonical_spelling_is_rewritten_not_lost() {
    for (text, expected) in [
        ("ơ\u{323}", 0),        // exactly what this charset writes
        ("a\u{302}\u{301}", 1), // full NFD — the shape was taken apart
        ("ấ", 1),               // precomposed — the tone was not separated
        ("á", 1),
    ] {
        let (_, unmapped, normalized) = from_combining(text);
        assert_eq!(unmapped, 0, "{text:?} loses nothing");
        assert_eq!(normalized, expected, "{text:?}");
    }
}

/// A second mark of the same kind is left behind rather than overwriting the
/// first. `apply_tone` *replaces*, so absorbing it would drop the sắc silently and
/// break `an_exact_conversion_is_reversible`.
#[test]
fn a_second_tone_mark_ends_the_letter() {
    let (out, unmapped, normalized) = from_combining("a\u{301}\u{300}");
    assert_eq!(out, "á\u{300}", "the second tone is a letter of its own");
    assert_eq!(unmapped, 1, "the orphan mark is the guess");
    assert_eq!(normalized, 0);
}

#[test]
fn a_second_shape_mark_ends_the_letter() {
    let (out, unmapped, _) = from_combining("a\u{306}\u{302}");
    assert_eq!(out, "ă\u{302}", "the ă is not re-shaped into â");
    assert_eq!(unmapped, 1);

    // The shape is already on the base character, so the mark repeating it is
    // just as much an orphan.
    let (out, unmapped, _) = from_combining("â\u{302}");
    assert_eq!(out, "â\u{302}");
    assert_eq!(unmapped, 1);
}

/// A mark with nothing before it is debris, not a letter. It must not go through
/// `Atom::from_char`, which would classify it as though it meant something.
#[test]
fn a_mark_with_no_base_passes_through_and_is_reported() {
    for text in ["\u{301}", "n\u{301}", "đ\u{301}", "Đ\u{323}", "5\u{300}"] {
        let (out, unmapped, _) = from_combining(text);
        assert_eq!(out, text, "{text:?} survives unchanged");
        assert_eq!(unmapped, 1, "{text:?}");
    }
}

/// Not every vowel takes every shape. The mark stays where it is.
#[test]
fn a_shape_a_vowel_cannot_take_is_not_applied() {
    for text in ["y\u{302}", "i\u{31b}", "e\u{306}", "u\u{302}", "a\u{31b}"] {
        let (out, unmapped, _) = from_combining(text);
        assert_eq!(out, text, "{text:?} should be unchanged");
        assert_eq!(unmapped, 1, "{text:?}");
    }
}

/// A base at the end of the text is a letter, not a truncated sequence.
#[test]
fn a_base_at_the_end_of_the_text_is_a_letter_on_its_own() {
    for text in ["a", "â", "ơ", "đ", "a\u{301}"] {
        let (out, unmapped, normalized) = from_combining(text);
        assert_eq!(out.chars().count(), 1, "{text:?} is one letter");
        assert_eq!((unmapped, normalized), (0, 0), "{text:?}");
    }
}

/// Two marks fill the letter; a third belongs to whatever comes next.
#[test]
fn a_third_mark_is_left_for_the_next_letter() {
    let (out, unmapped, normalized) = from_combining("a\u{302}\u{301}\u{323}");
    assert_eq!(out, "ấ\u{323}");
    assert_eq!(unmapped, 1, "the third mark is an orphan");
    assert_eq!(normalized, 1, "the letter itself was full NFD");
}

/// The strict half of the policy: reading accepts a decomposed shape, writing
/// never produces one.
#[test]
fn encode_never_emits_a_shape_mark() {
    for family in 0..vowels::FAMILY_COUNT {
        for tone in 0..6 {
            for upper in [false, true] {
                let c = vowels::vowel_glyph(family, tone, upper).expect("in inventory");
                let encoded = to_combining(&c.to_string()).0;
                for out in encoded.chars() {
                    assert!(
                        !matches!(out, '\u{302}' | '\u{306}' | '\u{31b}'),
                        "{c} was written with a shape mark"
                    );
                }
            }
        }
    }
}

/// `đ` has no canonical decomposition, so there is nothing to take apart.
#[test]
fn the_stroke_is_never_decomposed() {
    for c in ['đ', 'Đ'] {
        let (encoded, unmapped, normalized) = to_combining(&c.to_string());
        assert_eq!(encoded, c.to_string());
        assert_eq!((unmapped, normalized), (0, 0));
    }
}

/// Known and reported, rather than discovered in the field. A bare mark written
/// after a letter attaches itself to it, so the text reads back as something else
/// — the direct twin of VNI's `ø` fusing with the letter before it.
#[test]
fn a_combining_mark_passthrough_can_fuse_with_the_letter_before_it() {
    let (encoded, unmapped, _) = to_combining("a\u{301}");
    assert_eq!(unmapped, 1, "a stray mark cannot be written exactly");
    assert_eq!(from_combining(&encoded).0, "á", "the two fused");
}

/// The claim no legacy charset can make: this one is Unicode, so it spells
/// everything.
#[test]
fn a_non_vietnamese_character_is_spellable() {
    for c in ['₫', '日', 'ñ', '—'] {
        let (encoded, unmapped, normalized) = to_combining(&c.to_string());
        assert_eq!((unmapped, normalized), (0, 0), "{c}");
        assert_eq!(from_combining(&encoded).0, c.to_string(), "{c}");
    }
}

/// Uppercase differs only in the base character; the marks are the same.
#[test]
fn uppercase_is_the_same_marks_on_an_uppercase_base() {
    for family in 0..vowels::FAMILY_COUNT {
        for tone in 0..6 {
            let lower = vowels::vowel_glyph(family, tone, false).expect("in inventory");
            let upper = vowels::vowel_glyph(family, tone, true).expect("in inventory");
            let (lowered, raised) = (
                to_combining(&lower.to_string()).0,
                to_combining(&upper.to_string()).0,
            );
            assert_eq!(
                lowered.chars().skip(1).collect::<String>(),
                raised.chars().skip(1).collect::<String>(),
                "{upper} carries different marks from {lower}"
            );
            assert_eq!(raised.chars().count(), lowered.chars().count());
        }
    }
}

/// Two real words with their code points written out, so a table edit has to face
/// a concrete example and not only a property.
#[test]
fn a_real_word_encodes_to_the_code_points_a_unikey_document_holds() {
    let (encoded, unmapped, normalized) = to_combining("Việt Nam");
    assert_eq!((unmapped, normalized), (0, 0));
    assert_eq!(
        points(&encoded),
        vec![0x56, 0x69, 0xEA, 0x323, 0x74, 0x20, 0x4E, 0x61, 0x6D],
        "V i ê +nặng t _ N a m"
    );
    assert_eq!(from_combining(&encoded).0, "Việt Nam");

    let (encoded, unmapped, normalized) = to_combining("đường");
    assert_eq!((unmapped, normalized), (0, 0));
    assert_eq!(
        points(&encoded),
        vec![0x111, 0x1B0, 0x1A1, 0x300, 0x6E, 0x67],
        "đ ư ơ +huyền n g"
    );
    assert_eq!(from_combining(&encoded).0, "đường");
}
