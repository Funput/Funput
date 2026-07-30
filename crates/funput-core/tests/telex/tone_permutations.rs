//! A tone key must land on the same vowel wherever it is typed.
//!
//! Companion to `w_permutations.rs`, which does the same for the horn/breve
//! marker. Free tone placement is the composer's contract: `gias` and `gisa` are
//! both `giá`. The `gi`/`qu` onsets are the hard cases — their glide looks like a
//! nucleus vowel and can hold the tone as a transient — so they are covered here
//! together with plain syllables that must not regress.

use funput_core::{InputMethod, ToneStyle, apply};

struct Case {
    /// Key sequence with **no** tone key.
    base: &'static str,
    tone: char,
    target: &'static str,
}

/// First boundary a tone key can occupy: just past the first vowel. Before any
/// vowel a tone key is a plain letter (`hfoa` stays `hfoa`), not a mark.
fn first_boundary(base: &str) -> usize {
    base.chars()
        .position(|ch| "aeiouy".contains(ch))
        .map_or(base.chars().count(), |i| i + 1)
}

fn insert(base: &str, boundary: usize, tone: char) -> String {
    let offset = base
        .char_indices()
        .nth(boundary)
        .map_or(base.len(), |p| p.0);
    format!("{}{tone}{}", &base[..offset], &base[offset..])
}

fn type_keys(keys: &str, style: ToneStyle) -> String {
    keys.chars().fold(String::new(), |buffer, key| {
        apply(&buffer, key, InputMethod::Telex, style).text
    })
}

fn assert_converges(case: &Case, style: ToneStyle) {
    for boundary in first_boundary(case.base)..=case.base.chars().count() {
        let keys = insert(case.base, boundary, case.tone);
        assert_eq!(
            type_keys(&keys, style),
            case.target,
            "did not converge: {keys} ({style:?})"
        );
    }
}

const GLIDE_ONSETS: &[Case] = &[
    Case {
        base: "gia",
        tone: 's',
        target: "giá",
    },
    Case {
        base: "gio",
        tone: 'f',
        target: "giò",
    },
    Case {
        base: "gian",
        tone: 'r',
        target: "giản",
    },
    Case {
        base: "gieng",
        tone: 's',
        target: "giếng",
    },
    Case {
        base: "giuw",
        tone: 'x',
        target: "giữ",
    },
    Case {
        base: "giow",
        tone: 'f',
        target: "giờ",
    },
    Case {
        base: "qua",
        tone: 's',
        target: "quá",
    },
    Case {
        base: "quy",
        tone: 's',
        target: "quý",
    },
    Case {
        base: "quan",
        tone: 'f',
        target: "quàn",
    },
    Case {
        base: "quyeen",
        tone: 'f',
        target: "quyền",
    },
    Case {
        base: "quooc",
        tone: 's',
        target: "quốc",
    },
];

/// A lone glide vowel *is* the nucleus, so the tone stays on it.
const LONE_GLIDE_VOWEL: &[Case] = &[
    Case {
        base: "gi",
        tone: 'f',
        target: "gì",
    },
    Case {
        base: "gin",
        tone: 'f',
        target: "gìn",
    },
];

/// No glide involved — these already converged and must keep doing so.
const PLAIN_ONSETS: &[Case] = &[
    Case {
        base: "hoa",
        tone: 'f',
        target: "hòa",
    },
    Case {
        base: "toan",
        tone: 's',
        target: "toán",
    },
    Case {
        base: "nguoiw",
        tone: 'f',
        target: "người",
    },
    Case {
        base: "nghia",
        tone: 'x',
        target: "nghĩa",
    },
];

#[test]
fn glide_onsets_place_the_tone_on_the_nucleus() {
    for case in GLIDE_ONSETS {
        assert_converges(case, ToneStyle::Traditional);
    }
}

#[test]
fn lone_glide_vowel_keeps_the_tone() {
    for case in LONE_GLIDE_VOWEL {
        for style in [ToneStyle::Traditional, ToneStyle::Modern] {
            assert_converges(case, style);
        }
    }
}

#[test]
fn plain_onsets_still_converge() {
    for case in PLAIN_ONSETS {
        assert_converges(case, ToneStyle::Traditional);
    }
}

#[test]
fn glide_onsets_converge_in_modern_style_too() {
    // `oa`/`oe`/`uy` are the only style-dependent clusters, and behind a `qu`
    // glide there is no cluster left to disagree about: `quý` either way.
    for case in GLIDE_ONSETS {
        assert_converges(case, ToneStyle::Modern);
    }
}
