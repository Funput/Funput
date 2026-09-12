//! The examples `docs/features/text-case.md` promises, through the public API.
//!
//! Every other test in this feature reaches inside a module. These five are the
//! contract a reader of the doc is owed, and they are checked the way a shell will
//! call them: one `apply`, one `Transform`, default `Options`.

use funput_core::textcase::{Options, Transform, apply};

/// One row per line of the table at the top of the design doc.
const DOCUMENTED: [(Transform, &str, &str); 5] = [
    (Transform::Upper, "Xin chào Việt Nam", "XIN CHÀO VIỆT NAM"),
    (Transform::Lower, "Xin Chào Việt Nam", "xin chào việt nam"),
    (
        Transform::NoDiacritics,
        "Tiếng Việt rất đẹp",
        "Tieng Viet rat dep",
    ),
    (
        Transform::Sentence,
        "xin chào. hôm nay trời đẹp.",
        "Xin chào. Hôm nay trời đẹp.",
    ),
    (
        Transform::Title,
        "bàn phím tiếng Việt",
        "Bàn Phím Tiếng Việt",
    ),
];

#[test]
fn the_design_doc_examples_hold() {
    for (transform, input, expected) in DOCUMENTED {
        assert_eq!(
            apply(input, transform, Options::default()),
            expected,
            "{transform:?} on {input:?}"
        );
    }
}

/// `Tiếng Việt` in the combining spelling, and where each transform takes it. Written
/// out by hand because core carries no normalizer — that is the point of having it
/// here: a transform that quietly assumed precomposed input would fail this test and
/// pass every other one.
const COMBINING: &str = "Tie\u{302}\u{301}ng Vie\u{323}\u{302}t";

#[test]
fn the_combining_spelling_is_handled_without_being_normalized_away() {
    let options = Options::default();
    assert_eq!(
        apply(COMBINING, Transform::Upper, options),
        "TIE\u{302}\u{301}NG VIE\u{323}\u{302}T"
    );
    assert_eq!(
        apply(COMBINING, Transform::Lower, options),
        "tie\u{302}\u{301}ng vie\u{323}\u{302}t"
    );
    // Only bỏ dấu is allowed to collapse the two spellings, because ASCII has one.
    assert_eq!(
        apply(COMBINING, Transform::NoDiacritics, options),
        apply("Tiếng Việt", Transform::NoDiacritics, options)
    );
    assert_eq!(
        apply(COMBINING, Transform::Title, options),
        "Tie\u{302}\u{301}ng Vie\u{323}\u{302}t"
    );
}

/// The window lets a user press one transform and then another, so the interesting
/// pairs are the ones where the order changes the answer.
#[test]
fn transforms_stack() {
    let options = Options::default();
    let shouting = "GỬI VỀ TP. HCM";

    // Title case alone protects the all-caps words — that is what it is for — so
    // flattening first is how a user reaches the other answer.
    assert_eq!(apply(shouting, Transform::Title, options), shouting);
    let lowered = apply(shouting, Transform::Lower, options);
    assert_eq!(apply(&lowered, Transform::Title, options), "Gửi Về Tp. Hcm");

    // Bỏ dấu then UPPERCASE is the filename case, and it must not resurrect a tone.
    let bare = apply("Tiếng Việt", Transform::NoDiacritics, options);
    assert_eq!(apply(&bare, Transform::Upper, options), "TIENG VIET");
}
