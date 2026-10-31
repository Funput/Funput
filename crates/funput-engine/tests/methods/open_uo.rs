//! The open `uơ` rhyme, end to end. Every method reaches it on the way to `ường`
//! (`trưo` + a tone → `truờ`), but only `thuở`, `huơ`, `khuơ` and `uở` end on it,
//! so a word left there behind any other onset is English and is restored.

use funput_core::InputMethod;

use crate::support::app_text;

#[test]
fn full_telex_restores_english_left_on_an_open_uo() {
    // Issue #661: `tw` reads as `tư`, and `tưo` + `s` became `tuớ`.
    assert_eq!(
        app_text(InputMethod::TelexAdvanced, "twos thwor "),
        "twos thuở "
    );
    // The same rule ends the documented `giwof` → `giuờ` divergence: Full Telex
    // still cannot read that `w` as the horn of a later `o`, so the raw keys come
    // back instead of a non-word (`giowf` and `gi[f` type `giờ`).
    assert_eq!(app_text(InputMethod::TelexAdvanced, "giwof "), "giwof ");
}

#[test]
fn telex_keeps_the_attested_open_uo_and_passes_through_ương() {
    assert_eq!(
        app_text(
            InputMethod::Telex,
            "thuwor uwor khuwof truwofng tuwos nuwos "
        ),
        "thuở uở khuờ trường tuwos nuwos "
    );
}

#[test]
fn vni_digits_still_mean_vietnamese() {
    // A digit is a deliberate diacritic (`keystrokes_intend_vietnamese`), so English
    // restore never overrides it: the non-word stays as the user shaped it.
    assert_eq!(app_text(InputMethod::Vni, "tu7o1 "), "tuớ ");
    assert_eq!(app_text(InputMethod::Vni, "thu7o3 "), "thuở ");
}
