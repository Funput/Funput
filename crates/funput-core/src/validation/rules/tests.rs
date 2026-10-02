use super::*;

const STANDARD: SyllableRules = SyllableRules::STANDARD;
const ZFWJ: SyllableRules = STANDARD.with_extra_onsets(ExtraOnsets::ZFWJ);

#[test]
fn standard_is_the_default_and_admits_nothing_extra() {
    assert_eq!(SyllableRules::default(), STANDARD);
    assert_eq!(ExtraOnsets::default(), ExtraOnsets::NONE);
    assert!(STANDARD.extra_onsets.is_empty());
    assert!(!ExtraOnsets::NONE.admits("f"));
}

#[test]
fn onset_sets_combine_like_sets() {
    let letters = [
        ExtraOnsets::F,
        ExtraOnsets::J,
        ExtraOnsets::W,
        ExtraOnsets::Z,
    ];
    for letter in letters {
        assert!(!letter.is_empty());
        assert!(ExtraOnsets::ZFWJ.contains(letter));
        assert_eq!(letter.union(letter), letter);
    }
    let fj = ExtraOnsets::F.union(ExtraOnsets::J);
    assert!(fj.contains(ExtraOnsets::F) && fj.contains(ExtraOnsets::J));
    assert!(!fj.contains(ExtraOnsets::W) && !fj.contains(ExtraOnsets::ZFWJ));
    assert!(fj.contains(ExtraOnsets::NONE));
}

#[test]
fn admits_exactly_the_listed_spellings_in_any_case() {
    for prefix in ["f", "F", "j", "J", "w", "W", "z", "Z"] {
        assert!(ExtraOnsets::ZFWJ.admits(prefix), "{prefix}");
    }
    for prefix in ["", "a", "v", "đ", "Đ", "ff", "fl", "ph", "zh", "wr"] {
        assert!(!ExtraOnsets::ZFWJ.admits(prefix), "{prefix}");
    }
    assert!(ExtraOnsets::F.admits("F"));
    assert!(!ExtraOnsets::F.admits("w"));
}

#[test]
fn an_admitted_letter_parses_as_the_onset() {
    let parts = parse_syllable("fai", ZFWJ);
    assert_eq!(parts.onset, "f");
    assert!(!parts.invalid_onset);
    assert!(parts.is_well_ordered());
    assert!(parse_syllable("fai", STANDARD).invalid_onset);
    // Native onsets read the same either way.
    for (word, onset) in [("pha", "ph"), ("gia", "gi"), ("qua", "qu"), ("vô", "v")] {
        assert_eq!(parse_syllable(word, ZFWJ).onset, onset, "{word}");
    }
}

#[test]
fn an_extra_onset_never_starts_a_cluster() {
    // The second consonant sits between the onset and the vowel, so these stay
    // English: no rule ever reads `fl`, `fr`, `wh`, `wr` or `zh` as one onset.
    for word in ["flo", "fra", "whe", "wro", "zha"] {
        let parts = parse_syllable(word, ZFWJ);
        assert_eq!(parts.onset.chars().count(), 1, "{word}");
        assert!(!parts.is_well_ordered(), "{word}");
        assert!(ZFWJ.is_definitely_invalid(word), "{word}");
    }
}

#[test]
fn extra_onsets_open_complete_syllables() {
    for word in [
        "zô", "zố", "zui", "jờ", "Jút", "fải", "fan", "wá", "WÁ", "Zô",
    ] {
        assert!(ZFWJ.is_complete_syllable(word), "{word}");
        assert!(ZFWJ.is_valid(word), "{word}");
        assert!(!STANDARD.is_complete_syllable(word), "{word}");
        assert!(!STANDARD.is_valid(word), "{word}");
    }
}

#[test]
fn the_rhyme_must_still_be_vietnamese() {
    // Only the onset is widened: a coda Vietnamese lacks (`fôd`, `jump`, `fix`), a
    // rhyme it lacks (`wẻa`), a stop coda under huyền (`fòt`) or no nucleus at all
    // (`zt`) is as wrong after `f` as after `v`.
    for word in ["fôd", "jump", "fix", "wẻa", "fòt", "zt"] {
        assert!(!ZFWJ.is_complete_syllable(word), "{word}");
    }
    assert!(ZFWJ.is_definitely_invalid("zôd"));
    assert!(ZFWJ.is_definitely_invalid("fòt"));
}

#[test]
fn reachability_and_reopening_follow_the_rules() {
    for word in ["zô", "fa", "wu"] {
        assert!(!ZFWJ.is_definitely_invalid(word), "{word}");
        assert!(STANDARD.is_definitely_invalid(word), "{word}");
    }
    for method in [InputMethod::Telex, InputMethod::Vni] {
        assert!(!ZFWJ.is_definitely_invalid_in("jờ", method));
        assert!(STANDARD.is_definitely_invalid_in("jờ", method));
    }
    // A stop coda still awaiting its tone re-opens, as `chuc` does natively.
    assert!(ZFWJ.is_reopenable_syllable("jut"));
    assert!(!STANDARD.is_reopenable_syllable("jut"));
    assert!(!ZFWJ.is_reopenable_syllable("food"));
}

#[test]
fn each_letter_is_admitted_on_its_own() {
    let only_f = STANDARD.with_extra_onsets(ExtraOnsets::F);
    assert!(only_f.is_complete_syllable("fải"));
    for word in ["zô", "jờ", "wá"] {
        assert!(!only_f.is_complete_syllable(word), "{word}");
    }
}
