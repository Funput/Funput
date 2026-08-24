use super::*;

/// **The regression net for the codec fix this feature depended on.**
///
/// Reading precomposed Unicode as TCVN3 or VNI used to report nothing wrong, so all
/// three charsets scored identically and detection was impossible on the commonest
/// input there is. If this returns `None`, the byte-oriented codecs have gone back
/// to accepting characters no byte could hold.
#[test]
fn ordinary_unicode_is_detected_as_unicode() {
    assert_eq!(detect("Việt Nam"), Some(Charset::Unicode));
    assert_eq!(detect("Cộng hòa Xã hội Chủ nghĩa"), Some(Charset::Unicode));
}

#[test]
fn a_legacy_document_is_detected_by_the_letters_it_spells() {
    // `Việt Nam` as each legacy charset stores it.
    assert_eq!(detect("Vi\u{D6}t Nam"), Some(Charset::Tcvn3));
    assert_eq!(detect("Vi\u{65}\u{E4}t Nam"), Some(Charset::VniWindows));
}

#[test]
fn the_unikey_convention_and_full_nfd_both_read_as_combining() {
    assert_eq!(
        detect("Viê\u{323}t Nam"),
        Some(Charset::UnicodeCombining),
        "the UniKey spelling"
    );
    assert_eq!(
        detect("Vie\u{323}\u{302}t Nam"),
        Some(Charset::UnicodeCombining),
        "canonical NFD, which reads correctly and is rewritten"
    );
}

/// Text every charset spells the same way carries no evidence at all. Saying so is
/// more useful than picking one at random — and every answer would be correct.
#[test]
fn text_that_reads_the_same_everywhere_is_not_detected() {
    for text in ["Ha Noi 1945", "cho toi mot ly ca phe", ""] {
        assert_eq!(detect(text), None, "{text:?}");
    }
}

/// `is_complete_syllable` accepts a fair amount of English (`the`, `can`, `long`),
/// so an English document scores well — but it scores *equally* well under every
/// candidate, which is what actually rules it out.
#[test]
fn an_english_document_is_not_detected() {
    assert_eq!(detect("The quick brown fox jumps over the lazy dog"), None);
}

#[test]
fn text_with_no_words_at_all_is_not_detected() {
    for text in ["   ", "1945 2024 30/4", "!!! ??? ..."] {
        assert_eq!(detect(text), None, "{text:?}");
    }
}

/// Sixteen bytes are a letter in both legacy charsets, so a single short word can
/// be genuinely undecidable. `0xF4` is `ụ` in TCVN3 and `ơ` in VNI, and `cụ` and
/// `cơ` are both real words.
#[test]
fn a_word_two_charsets_spell_the_same_way_is_not_detected() {
    assert_eq!(detect("c\u{F4}"), None, "cụ or cơ — no way to tell");
    assert_eq!(detect("t\u{F6}"), None, "tử or tư");
}

/// Vietnamese official documents are full of all-caps headings, and the syllable
/// checker is case-insensitive, so they detect like anything else.
#[test]
fn an_all_caps_legacy_heading_is_detected() {
    assert_eq!(detect("H\u{B5} N\u{E9}I"), Some(Charset::Tcvn3));
}

/// The shape of a `gõ tắt` file: `trigger:expansion`, no space after the colon.
/// This is what the word splitter exists for — splitting on whitespace alone would
/// hand the scorer `vn:vi\u{D6}t` and lose the Vietnamese word inside every line.
#[test]
fn a_unikey_macro_file_is_detected_from_its_bytes() {
    let file = b"vn:vi\xD6t nam\r\nhn:h\xB5 n\xE9i\r\n";
    assert_eq!(detect_bytes(file), Some(Charset::Tcvn3));
}

#[test]
fn a_utf8_file_is_detected_from_its_bytes() {
    assert_eq!(detect_bytes("Việt Nam".as_bytes()), Some(Charset::Unicode));
    assert_eq!(
        detect_bytes("Viê\u{323}t Nam".as_bytes()),
        Some(Charset::UnicodeCombining)
    );
}

/// The two doors answer differently on the same content, and that is the point
/// rather than an oversight: bytes can be invalid UTF-8, which is evidence against
/// the Unicode charsets. By the time `detect` holds a `&str` the damage has been
/// repaired and the evidence is gone.
#[test]
fn the_two_doors_may_disagree_and_that_is_intended() {
    let bytes = b"vi\xD6t nam";
    let as_chars: String = bytes.iter().copied().map(char::from).collect();

    assert_eq!(detect_bytes(bytes), Some(Charset::Tcvn3));
    assert_eq!(detect(&as_chars), Some(Charset::Tcvn3));

    // Latin-1 that is *also* Vietnamese under no charset in particular: the byte
    // door sees the UTF-8 damage, the text door never had it.
    assert_eq!(detect_bytes(&[0xFF, 0xFE, 0x41]), None);
}

/// **A known limitation, pinned so the next reader finds it already known.**
///
/// VISCII is Latin-1-compatible for the letters it shares with TCVN3, and nobody
/// has implemented it. Every judgement here is relative — the best of four
/// hypotheses — so an unimplemented charset is always answered with its nearest
/// neighbour rather than refused. `c\u{E0} ph\u{EA}` is `cà phê` in VISCII and
/// converts to `cà phờ` under the answer given here.
///
/// No threshold fixes this; implementing VISCII does.
#[test]
fn an_unimplemented_charset_is_answered_with_its_nearest_neighbour() {
    let viscii = "c\u{E0} ph\u{EA}";
    assert_eq!(
        detect(viscii),
        Some(Charset::Unicode),
        "read as Latin-1 this happens to be right, by luck rather than judgement"
    );
    assert_eq!(
        detect_bytes(b"c\xE0 ph\xEA"),
        Some(Charset::Tcvn3),
        "and as bytes it is confidently wrong — see the doc comment"
    );
}

#[test]
fn a_prefix_is_enough_and_a_long_document_is_not_read_whole() {
    let long = "Việt Nam ".repeat(4096);
    assert!(long.len() > PREFIX);
    assert_eq!(detect(&long), Some(Charset::Unicode));
}

#[test]
fn the_prefix_cut_lands_on_a_character_boundary() {
    // Multi-byte characters straddling the cut must not panic or split.
    let text = "ệ".repeat(PREFIX);
    assert!(prefix(&text).len() <= PREFIX);
    assert!(text.starts_with(prefix(&text)));
}
