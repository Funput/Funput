//! A mũ key after a circumflex the composer added confirms it instead of reverting
//! it: `viecje` → `việc`, like `viecj`. Design: `docs/features/implied-circumflex-confirm.md`.

use funput_core::InputMethod;
use funput_engine::{Action, Engine};

use crate::support::{
    Document, app_text, app_text_in, correcting_engine, type_keys_buffer, type_touched,
};

fn assert_all(method: InputMethod, cases: &[(&str, &str)]) {
    for &(keys, expected) in cases {
        assert_eq!(
            type_keys_buffer(method, keys),
            expected,
            "typing {keys:?} in {method:?}"
        );
    }
}

#[test]
fn the_mu_key_after_a_tone_confirms_the_implied_circumflex() {
    assert_all(
        InputMethod::Telex,
        &[
            ("viecje", "việc"),
            ("viejce", "việc"),
            ("tiengse", "tiếng"),
            ("dienje", "diện"),
            ("vietje", "việt"),
            ("giengse", "giếng"),
            ("kiemse", "kiếm"),
            ("nghiengfe", "nghiềng"),
            ("hieure", "hiểu"),
            ("chieufe", "chiều"),
            ("giemfe", "giềm"),
        ],
    );
}

#[test]
fn uppercase_confirms_too() {
    assert_all(
        InputMethod::Telex,
        &[("VIECJE", "VIỆC"), ("Viecje", "Việc"), ("TIENGSE", "TIẾNG")],
    );
}

#[test]
fn the_confirming_key_is_swallowed_and_kept_among_the_raw_keys() {
    let mut engine = Engine::new();
    engine.set_method(InputMethod::Telex);
    for key in "viecj".chars() {
        engine.process_char(key);
    }
    let result = engine.process_char('e');
    assert_eq!(result.action, Action::Send);
    assert_eq!(result.backspace, 0);
    assert_eq!(result.output, "");
    assert_eq!(engine.buffer(), "việc");
    assert_eq!(engine.keys(), "viecje");
}

#[test]
fn a_circumflex_the_user_typed_still_reverts() {
    assert_all(
        InputMethod::Telex,
        &[
            ("vieecje", "viẹce"),
            ("vieecj", "việc"),
            ("viecej", "việc"),
            ("vieejc", "việc"),
            ("tienges", "tiếng"),
        ],
    );
}

#[test]
fn a_second_mu_key_after_a_confirm_reverts() {
    assert_all(
        InputMethod::Telex,
        &[
            ("viecjee", "viẹce"),
            ("viecjeee", "viẹcee"),
            ("chieufee", "chièue"),
        ],
    );
}

#[test]
fn the_gi_rhyme_rule_is_untouched() {
    assert_all(
        InputMethod::Telex,
        &[("giefm", "gièm"), ("giemf", "giềm"), ("giefme", "giềm")],
    );
}

#[test]
fn ye_and_uye_rhymes_are_untouched() {
    assert_all(
        InputMethod::Telex,
        &[
            ("yeenr", "yển"),
            ("tuyenre", "tuyển"),
            ("duyjete", "duyệt"),
            ("duyjetee", "duyẹte"),
        ],
    );
}

#[test]
fn telex_advanced_follows_telex() {
    assert_all(
        InputMethod::TelexAdvanced,
        &[
            ("viecje", "việc"),
            ("tiengse", "tiếng"),
            ("vieecje", "viẹce"),
            ("viecjee", "viẹce"),
        ],
    );
}

#[test]
fn vni_confirms_on_the_first_six() {
    assert_all(
        InputMethod::Vni,
        &[
            ("viec56", "việc"),
            ("vie5c6", "việc"),
            ("dien56", "diện"),
            ("tieng16", "tiếng"),
            ("chieu26", "chiều"),
            ("VIEC56", "VIỆC"),
            ("vie6c56", "viẹc6"),
            ("viec65", "việc"),
            ("viec5", "việc"),
            ("dong16", "dống"),
            ("muon56", "muộn"),
        ],
    );
}

#[test]
fn flip_shows_every_key_typed() {
    let mut engine = Engine::new();
    engine.set_method(InputMethod::Telex);
    for key in "viecje".chars() {
        engine.process_char(key);
    }
    engine.flip_composing();
    assert_eq!(engine.buffer(), "viecje");
    engine.flip_composing();
    assert_eq!(engine.buffer(), "việc");
}

/// Backspace rewrites the raw keys from the screen, so the word is treated as one
/// whose mũ the user typed: the next `e` reverts, exactly as it did before the fix
/// (`viecj` + Backspace + `e` has never confirmed anything).
#[test]
fn backspace_after_a_confirmation_keeps_the_old_revert() {
    let mut confirmed = Engine::new();
    confirmed.set_method(InputMethod::Telex);
    for key in "viecje".chars() {
        confirmed.process_char(key);
    }
    confirmed.on_backspace();
    assert_eq!(confirmed.buffer(), "việ");
    confirmed.process_char('e');

    let mut plain = Engine::new();
    plain.set_method(InputMethod::Telex);
    for key in "viecj".chars() {
        plain.process_char(key);
    }
    plain.on_backspace();
    plain.process_char('e');

    assert_eq!(confirmed.buffer(), plain.buffer());
    assert_eq!(confirmed.buffer(), "viẹe");
}

#[test]
fn an_adopted_word_keeps_the_old_revert() {
    let mut engine = Engine::new();
    engine.set_method(InputMethod::Telex);
    assert!(engine.adopt("việc"));
    engine.process_char('e');
    assert_eq!(engine.buffer(), "viẹce");
}

/// English words in which the mũ key does land on a promoted `ê`: the word on
/// screen then has no standard onset (`Iển`, `quiếc`, `priến`, `briền`) or is not a
/// syllable (`diểct`), so the key reverts and the word comes out as it did before
/// the fix — with eager restore on and off.
#[test]
fn english_that_reaches_the_confirm_path_is_untouched() {
    for (word, eager_on, eager_off) in [
        ("Irene ", "Irene ", "Irene "),
        ("irene ", "irene ", "irene "),
        ("quiesce ", "quiesce ", "quiéce "),
        ("quiescent ", "quiescent ", "quiescent "),
        ("priestess ", "priestess ", "priestess "),
        ("disrespect ", "disrespect ", "disrespect "),
        ("briefness ", "brièness ", "briênes "),
        ("directed ", "directed ", "directed "),
    ] {
        assert_eq!(
            app_text(InputMethod::Telex, word),
            eager_on,
            "{word:?}, eager on"
        );
        let mut engine = Engine::new();
        engine.set_method(InputMethod::Telex);
        engine.update_config(|config| config.eager_restore = false);
        assert_eq!(
            app_text_in(&mut engine, word),
            eager_off,
            "{word:?}, eager off"
        );
    }
}

#[test]
fn typo_correction_sees_the_whole_word() {
    let mut engine = correcting_engine(InputMethod::Telex);
    let mut doc = Document::new();
    type_touched(&mut engine, &mut doc, "viecje ", &[]);
    assert_eq!(doc.text(), "việc ");
}

/// Known side effects of swallowing the key, recorded so a change shows up. The
/// key after a confirmation acts on the toned word, not on a reverted one.
#[test]
fn known_side_effects() {
    assert_all(
        InputMethod::Telex,
        &[("viecjse", "viếc"), ("viecjej", "viêcj")],
    );
    assert_all(
        InputMethod::Vni,
        &[("viec566", "viẹc6"), ("viec516", "viếc")],
    );
}

/// The word on screen has to be a complete syllable: a stop coda without a tone
/// (`viêc` after `z`) is not one, and the mũ key reverts as before.
#[test]
fn an_incomplete_word_keeps_the_revert() {
    assert_eq!(type_keys_buffer(InputMethod::Telex, "viecjze"), "viece");
    assert_eq!(type_keys_buffer(InputMethod::Vni, "viec506"), "viec6");
    // `giếc` is accepted by no rhyme table, so `giecse` reverts to `giéce` and the
    // boundary leaves it alone, exactly as before.
    assert_eq!(app_text(InputMethod::Telex, "giecse "), "giéce ");
    assert_eq!(app_text(InputMethod::Vni, "giec16 "), "giéc6 ");
}

/// English words with `ie` + consonant + `e` show no complete syllable when the
/// `e` lands (`diểct`), so they revert as before — which is what opens the
/// doubled-key restore that brings them back with eager restore off.
#[test]
fn english_with_ie_still_restores_with_eager_restore_off() {
    for word in ["directed ", "disengaged ", "fierce ", "lifetime "] {
        let mut engine = Engine::new();
        engine.set_method(InputMethod::Telex);
        engine.update_config(|config| config.eager_restore = false);
        assert_eq!(app_text_in(&mut engine, word), word);
    }
}
