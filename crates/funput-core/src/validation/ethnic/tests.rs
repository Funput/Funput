use super::*;
use crate::validation::reachability::{is_definitely_invalid, is_definitely_invalid_in};
use crate::validation::rhyme::has_prefix;
use crate::{InputMethod, is_complete_syllable, is_reopenable_syllable};

#[test]
fn cluster_kinds() {
    // `đr` is typed `dr` + a later `d` in Telex, so it shares English's `dr`.
    for shared in ["kr", "Kr", "KR", "pl", "dr", "đr", "Đr", "phl", "bh"] {
        assert_eq!(cluster_kind(shared), Some(ClusterKind::Shared), "{shared}");
    }
    for distinct in ["kp", "Kp", "KT", "hr", "xt", "kđr", "hđr"] {
        assert_eq!(
            cluster_kind(distinct),
            Some(ClusterKind::Distinct),
            "{distinct}"
        );
    }
    // Native onsets are not clusters; `kn`/`sl` stay out (`know`, `slow`).
    for none in ["", "k", "tr", "ngh", "kn", "sl", "st"] {
        assert_eq!(cluster_kind(none), None, "{none}");
    }
}

#[test]
fn name_finals_follow_a_vowel_vietnamese_closes() {
    let closes =
        |nucleus: &str, last: char| closes_with_name_final(nucleus.chars(), &[last], has_prefix);
    for (nucleus, last) in [("ă", 'h'), ("ư", 'h'), ("ê", 'h'), ("uô", 'l'), ("ơ", 'r')] {
        assert!(closes(nucleus, last), "{nucleus}{last} should close");
    }
    // `uôi` takes no coda in Vietnamese; `b` is no name final; one letter only.
    assert!(!closes("uôi", 'h'));
    assert!(!closes("", 'h'));
    assert!(!closes("ă", 'b'));
    assert!(!closes_with_name_final(
        "ă".chars(),
        &['h', 'h'],
        has_prefix
    ));
}

#[test]
fn name_finals_live_in_vni_only() {
    for name in ["păh", "Pưh", "đêh", "tẻh", "nuôl", "blơr", "cuôr"] {
        assert!(
            !is_definitely_invalid_in(name, InputMethod::Vni),
            "{name} should live in VNI"
        );
        assert!(
            is_definitely_invalid_in(name, InputMethod::Telex),
            "{name} is English-shaped in Telex"
        );
    }
    // Digits glued to a word still die in VNI: `a1b`, `u23vn`, `e2e`, `a4paper`.
    for dead in ["áb", "ủv", "èe", "ãp"] {
        assert!(
            is_definitely_invalid_in(dead, InputMethod::Vni),
            "{dead} should die"
        );
    }
}

#[test]
fn place_names_are_complete_syllables() {
    for name in [
        "kpă", "Kpăng", "dliê", "hrê", "xrê", "Xtiêng", "rcăm", "rlâm", "mrơn", "ktlê", "khlá",
        "phlắc", "phlạo", "tbuăn", "đrá", "Đrắk", "kđrao", "Hđrung", "kdăm", "rsươm",
    ] {
        assert!(is_complete_syllable(name), "{name} should be complete");
        assert!(!is_definitely_invalid(name), "{name} should stay alive");
    }
}

#[test]
fn english_stays_restorable() {
    // A shared cluster still needs a Vietnamese rhyme: Telex `draw` → `dră`.
    for word in ["dră", "blă", "kră", "drie"] {
        assert!(!is_complete_syllable(word), "{word} should not be complete");
    }
    // Finals English would collide with stay out, after any onset.
    for word in ["côl", "tôr", "blơr", "cáh", "âh", "kpăl"] {
        assert!(!is_complete_syllable(word), "{word} should not be complete");
        assert!(!is_reopenable_syllable(word), "{word} should be refused");
    }
    // A distinct cluster still needs an order Vietnamese has and a real final.
    for dead in ["kpăd", "kpna", "hrêl"] {
        assert!(is_definitely_invalid(dead), "{dead} should be a dead end");
    }
}
