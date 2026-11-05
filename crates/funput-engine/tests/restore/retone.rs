//! A nặng key right after an eager restore re-tones the word (#682): `hõc` dead-ends
//! on the `c` and shows `hoxc`, but the `j` that follows means `học`, as in UniKey.
//! Only nặng rescues — the other tone keys after a restore are English far more
//! often (`parts`, `texts`) than a slipped tone.

use funput_core::InputMethod;
use funput_engine::{Action, Engine, KeySource};

use crate::support::{app_text, app_text_in, type_keys_with_results};

fn telex(keys: &str) -> String {
    app_text(InputMethod::Telex, keys)
}

fn vni(keys: &str) -> String {
    app_text(InputMethod::Vni, keys)
}

fn engine(method: InputMethod) -> Engine {
    let mut engine = Engine::new();
    engine.set_method(method);
    engine
}

#[test]
fn nang_after_a_restored_stop_coda_gives_the_syllable() {
    for (keys, word) in [
        ("dduoxwcj", "được"),
        ("hoxcj", "học"),
        ("hofcj", "học"),
        ("horcj", "học"),
        ("taxtj", "tạt"),
        ("hoxpj", "họp"),
        ("thixchj", "thịch"),
        ("Hoxcj", "Học"),
        ("HOXCJ", "HỌC"),
    ] {
        assert_eq!(telex(keys), word, "{keys}");
    }
    for (keys, word) in [("d9uo37c5", "được"), ("ho3c5", "học"), ("ho2p5", "họp")] {
        assert_eq!(vni(keys), word, "{keys}");
    }
    assert_eq!(app_text(InputMethod::TelexAdvanced, "dduoxwcj"), "được");
}

#[test]
fn the_h_of_a_ch_coda_may_sit_between_the_restore_and_the_nang() {
    // `sãc` restores on the `c`; the `h` keeps the abandoned composition in step so
    // the nặng still lands. A nặng typed before the `h` never needed the rescue.
    for (keys, word) in [("saxchj", "sạch"), ("sachxj", "sạch"), ("saxcjh", "sạch")] {
        assert_eq!(telex(keys), word, "{keys}");
    }
    assert_eq!(vni("sa4ch5"), "sạch");
}

#[test]
fn the_rescued_word_survives_the_boundary_and_keeps_its_raw_keys() {
    assert_eq!(telex("dduoxwcj "), "được ");
    assert_eq!(vni("d9uo37c5 "), "được ");
    let (buffer, results) = type_keys_with_results(InputMethod::Telex, "dduoxwcj");
    assert_eq!(buffer, "được");
    let rescue = results.last().unwrap();
    assert_eq!(rescue.action, Action::Send);
    assert_eq!((rescue.backspace, rescue.output.as_str()), (7, "được"));
}

#[test]
fn only_the_nang_key_rescues() {
    // Sắc would be the other tone a stop coda takes, but after a restore it is
    // English (`parts`, `texts`, `gifts`) thirty times over in the lexicon.
    assert_eq!(telex("hofcs"), "hofcs");
    assert_eq!(vni("ho2c1"), "ho2c1");
    for word in ["parts ", "texts ", "gifts ", "sorts ", "hurts ", "marks "] {
        assert_eq!(telex(word), word);
    }
}

#[test]
fn english_words_that_restore_eagerly_still_type_as_english() {
    for word in ["text ", "next ", "fact ", "act ", "dict "] {
        assert_eq!(telex(word), word);
    }
}

#[test]
fn only_a_word_the_boundary_would_keep_is_rescued() {
    // `gịc` is no syllable: left on screen it would be restored at the boundary
    // anyway, so the rescue declines and the word stays raw throughout. `đuọc` is
    // one shape key short of `được`, so it is rescued and the `w` finishes it.
    assert_eq!(telex("gixcj "), "gixcj ");
    assert_eq!(vni("gi4c5 "), "gi4c5 ");
    assert_eq!(telex("dduoxcjw "), "được ");
    assert_eq!(vni("d9uo3c57 "), "được ");
    // Rescued but left unfinished, the word meets the boundary like any other
    // unfinished word: `nuọc` is restored there exactly as `nuocj ` is today.
    assert_eq!(telex("nuoxcj "), "nuoxcj ");
    assert_eq!(telex("nuocj "), "nuocj ");
}

#[test]
fn vni_digits_that_spell_a_slip_and_its_repair_compose() {
    // A wrong tone digit then `5` reads as a slipped tone in VNI, as in UniKey —
    // the chosen reading, even when the letters happen to spell English.
    assert_eq!(vni("mac35 "), "mạc ");
    assert_eq!(vni("tet45 "), "tẹt ");
}

#[test]
fn english_ch_words_are_left_alone() {
    // These never composed anything, so there is no shadow to follow.
    for word in ["beach ", "each ", "teach ", "touch ", "which "] {
        assert_eq!(telex(word), word);
    }
    // Here the `r` tones the vowel, the `c` restores and the `h` does cast a
    // shadow (`ảch`), which nothing ever re-tones.
    for word in [
        "arch ",
        "march ",
        "church ",
        "torch ",
        "porch ",
        "orchestra ",
        "search ",
    ] {
        assert_eq!(telex(word), word);
    }
}

#[test]
fn a_shadowed_ch_that_makes_no_syllable_is_not_rescued() {
    // `họch` has no rhyme, so the nặng after the shadow is refused and stays raw.
    assert_eq!(telex("hoxchj "), "hoxchj ");
    assert_eq!(vni("ho3ch5 "), "ho3ch5 ");
}

#[test]
fn a_flip_to_raw_keys_stops_the_shadow_and_the_rescue() {
    // The user pinned the raw form on `sax`; the `c` and `h` are typed on it, so
    // nothing is shadowed and the `j` appends. Flip has nothing to swap any more.
    let mut e = engine(InputMethod::Telex);
    assert_eq!(app_text_in(&mut e, "sax"), "sã");
    e.flip_composing();
    assert_eq!(e.buffer(), "sax");
    app_text_in(&mut e, "chj");
    assert_eq!(e.buffer(), "saxchj");
    assert_eq!(e.flip_composing().action, Action::None);
}

#[test]
fn flip_after_a_shadowed_h_recovers_the_vietnamese_coda() {
    // The shadow is what Flip shows: `sãch`, not the `sãc` the restore left behind.
    let mut e = engine(InputMethod::Telex);
    assert_eq!(app_text_in(&mut e, "saxch"), "saxch");
    e.flip_composing();
    assert_eq!(e.buffer(), "sãch");
}

#[test]
fn a_vni_name_final_is_not_a_rescue() {
    // `Pah8` restores like `bar8`; `5` on it would be `Pạh`, which is no finished
    // syllable, so the digit stays a digit (Flip still gives `Păh`).
    assert_eq!(vni("Pah85"), "Pah85");
}

#[test]
fn a_numpad_digit_is_a_number_not_a_rescue() {
    let mut e = engine(InputMethod::Vni);
    let mut text = app_text_in(&mut e, "ho3c");
    let digit = e.process_key('5', KeySource::Numpad);
    assert_eq!(digit.action, Action::None, "{digit:?}");
    text.push('5');
    assert_eq!(text, "ho3c5");
}

#[test]
fn flip_after_a_rescue_toggles_between_raw_keys_and_the_syllable() {
    let mut e = engine(InputMethod::Telex);
    assert_eq!(app_text_in(&mut e, "hoxcj"), "học");
    e.flip_composing();
    assert_eq!(e.buffer(), "hoxcj");
    e.flip_composing();
    assert_eq!(e.buffer(), "học");
}

#[test]
fn backspace_after_a_rescue_deletes_one_character_of_the_syllable() {
    let mut e = engine(InputMethod::Telex);
    assert_eq!(app_text_in(&mut e, "hoxcj"), "học");
    e.on_backspace();
    assert_eq!(e.buffer(), "họ");
    assert_eq!(e.keys(), "họ");
    e.process_char('c');
    assert_eq!(e.buffer(), "học");
}

#[test]
fn every_toggle_setup_reaches_the_syllable() {
    // With eager restore off the word never left its Vietnamese form, so the core
    // re-tones it on its own; with it on, the rescue brings the same result.
    for (smart, eager) in [(true, true), (true, false), (false, false)] {
        let mut e = engine(InputMethod::Telex);
        e.update_config(|c| {
            c.smart_restore = smart;
            c.eager_restore = eager;
        });
        assert_eq!(
            app_text_in(&mut e, "hoxcj"),
            "học",
            "smart {smart} eager {eager}"
        );
    }
}
