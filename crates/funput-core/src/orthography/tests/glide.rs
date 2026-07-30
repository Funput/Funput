use crate::orthography::glide::{self, Glide};

#[test]
fn plain_glides_are_recognised() {
    assert_eq!(glide::after("q", 'u'), Some(Glide::Qu));
    assert_eq!(glide::after("g", 'i'), Some(Glide::Gi));
    assert_eq!(glide::after("Q", 'U'), Some(Glide::Qu));
    assert_eq!(glide::after("G", 'I'), Some(Glide::Gi));
}

#[test]
fn toned_glides_are_recognised() {
    // The transient a tone key typed before the nucleus leaves behind — the case
    // that used to escape recognition and strand the tone (`gía`, `qúa`).
    for vowel in ['í', 'ì', 'ỉ', 'ĩ', 'ị', 'Í', 'Ị'] {
        assert_eq!(glide::after("g", vowel), Some(Glide::Gi), "{vowel}");
    }
    for vowel in ['ú', 'ù', 'ủ', 'ũ', 'ụ', 'Ú', 'Ụ'] {
        assert_eq!(glide::after("q", vowel), Some(Glide::Qu), "{vowel}");
    }
}

#[test]
fn shaped_vowels_are_not_glides() {
    // `vowel_stem` keeps the shape, so a horned `ư` never passes as the `u` of `qu`.
    for vowel in ['ư', 'ứ', 'ừ', 'Ư'] {
        assert_eq!(glide::after("q", vowel), None, "{vowel}");
    }
}

#[test]
fn the_consonant_must_be_the_whole_onset() {
    // `ngi`/`nghi` is `ng`/`ngh` + nucleus `i` (nghĩa), not a `gi` onset that
    // happens to end in `g` + `i`.
    assert_eq!(glide::after("ng", 'i'), None);
    assert_eq!(glide::after("ngh", 'i'), None);
    assert_eq!(glide::after("Ng", 'ì'), None);
}

#[test]
fn wrong_consonant_or_vowel_is_not_a_glide() {
    assert_eq!(glide::after("g", 'a'), None); // `ga` — plain nucleus
    assert_eq!(glide::after("q", 'a'), None);
    assert_eq!(glide::after("t", 'u'), None); // `tu` — nucleus, not a glide
    assert_eq!(glide::after("h", 'i'), None);
    assert_eq!(glide::after("g", 'n'), None); // not a vowel at all
    assert_eq!(glide::after("", 'u'), None); // word-initial `u` has no onset
}

#[test]
fn at_locates_the_glide_by_byte_offset() {
    assert_eq!(glide::at("qua", 1), Some(Glide::Qu));
    assert_eq!(glide::at("gia", 1), Some(Glide::Gi));
    assert_eq!(glide::at("gía", 1), Some(Glide::Gi)); // toned glide, multi-byte
    assert_eq!(glide::at("qua", 2), None); // the nucleus `a`
    assert_eq!(glide::at("qua", 0), None); // the onset consonant itself
    assert_eq!(glide::at("nghiêng", 3), None); // `ngh` + nucleus
}

#[test]
fn in_onset_matches_whole_onsets() {
    assert_eq!(glide::in_onset("qu"), Some(Glide::Qu));
    assert_eq!(glide::in_onset("gi"), Some(Glide::Gi));
    assert_eq!(glide::in_onset("qú"), Some(Glide::Qu));
    assert_eq!(glide::in_onset("gí"), Some(Glide::Gi));
    assert_eq!(glide::in_onset("Gì"), Some(Glide::Gi));

    assert_eq!(glide::in_onset("g"), None); // incomplete
    assert_eq!(glide::in_onset("gia"), None); // onset plus nucleus
    assert_eq!(glide::in_onset("ng"), None);
    assert_eq!(glide::in_onset("ngh"), None);
    assert_eq!(glide::in_onset(""), None);
}
