use funput_core::ExtraOnsets;

use super::*;

#[test]
fn every_letter_round_trips_through_its_symbol() {
    for letter in OnsetLetter::ALL {
        assert_eq!(OnsetLetter::from_symbol(letter.symbol()), Some(letter));
        let upper = letter.symbol().to_ascii_uppercase();
        assert_eq!(OnsetLetter::from_symbol(upper), Some(letter));
    }
    assert_eq!(OnsetLetter::from_symbol('q'), None);
}

#[test]
fn all_is_every_letter() {
    let every = OnsetLetter::ALL
        .into_iter()
        .fold(ExtraOnsetLetters::NONE, |set, l| set.with(l, true));
    assert_eq!(every, ExtraOnsetLetters::ALL);
}

#[test]
fn each_letter_admits_its_own_onset_in_the_engine() {
    let only = |letter| ExtraOnsetLetters::NONE.with(letter, true).core();
    assert_eq!(only(OnsetLetter::Z), ExtraOnsets::Z);
    assert_eq!(only(OnsetLetter::F), ExtraOnsets::F);
    assert_eq!(only(OnsetLetter::W), ExtraOnsets::W);
    assert_eq!(only(OnsetLetter::J), ExtraOnsets::J);
    assert_eq!(ExtraOnsetLetters::ALL.core(), ExtraOnsets::ZFWJ);
    assert_eq!(ExtraOnsetLetters::NONE.core(), ExtraOnsets::NONE);
}

#[test]
fn with_adds_and_removes_one_letter_and_leaves_the_rest() {
    let zj = ExtraOnsetLetters::NONE
        .with(OnsetLetter::J, true)
        .with(OnsetLetter::Z, true);
    assert!(zj.contains(OnsetLetter::Z) && zj.contains(OnsetLetter::J));
    assert!(!zj.contains(OnsetLetter::F));

    let j = zj.with(OnsetLetter::Z, false);
    assert!(!j.contains(OnsetLetter::Z) && j.contains(OnsetLetter::J));
    assert!(j.with(OnsetLetter::J, false).is_empty());
}

#[test]
fn id_spells_the_letters_in_settings_order() {
    let jz = ExtraOnsetLetters::from_id("jz");
    assert_eq!(jz.id(), "zj");
    assert_eq!(ExtraOnsetLetters::ALL.id(), "zfwj");
    assert_eq!(ExtraOnsetLetters::NONE.id(), "");
}

#[test]
fn from_id_ignores_case_repeats_and_strangers() {
    let zj = ExtraOnsetLetters::from_id("zj");
    assert_eq!(ExtraOnsetLetters::from_id("JZ"), zj);
    assert_eq!(ExtraOnsetLetters::from_id("zzjj"), zj);
    assert_eq!(ExtraOnsetLetters::from_id("zxq").id(), "z");
    assert_eq!(ExtraOnsetLetters::from_id(""), ExtraOnsetLetters::NONE);
    assert_eq!(
        ExtraOnsetLetters::from_id("dz?"),
        ExtraOnsetLetters::from_id("z")
    );
}

#[test]
fn serializes_as_the_spelled_out_letters() {
    let fj = ExtraOnsetLetters::from_id("fj");
    assert_eq!(serde_json::to_string(&fj).unwrap(), r#""fj""#);
    let back: ExtraOnsetLetters = serde_json::from_str(r#""JF""#).unwrap();
    assert_eq!(back, fj);
}
