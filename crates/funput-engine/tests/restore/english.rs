use funput_core::InputMethod;
use funput_engine::{Action, Engine};

#[test]
fn telex_valid_vn_keeps_composed_on_space() {
    assert_eq!(crate::support::app_text(InputMethod::Telex, "mas "), "má ");
}

#[test]
fn telex_english_restore_on_space() {
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, "absc "),
        "absc "
    );
}

#[test]
fn telex_pass_through_english_no_restore() {
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, "file "),
        "file "
    );
}

#[test]
fn telex_english_restore_on_punctuation() {
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, "absc,"),
        "absc,"
    );
}

#[test]
fn revert_then_space_keeps_reverted_word() {
    // "mix" → "mĩ"; double `x` reverts to "mix"; pressing Space must NOT re-restore
    // the stale raw keystrokes ("mixx"). The revert is the user's final intent.
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, "mixx "),
        "mix "
    );
    assert_eq!(crate::support::app_text(InputMethod::Vni, "a11 "), "a1 ");
}

#[test]
fn real_words_the_rhyme_table_once_missed_survive_the_boundary() {
    // `giếc` (`gi` + `iêc`) and the `ưm` interjections were restored to their raw
    // keys on Space, in every key order (#757).
    for (keys, expected) in [
        ("gieces", "giếc"),
        ("gieecs", "giếc"),
        ("gieesc", "giếc"),
        ("giesce", "giếc"),
        ("giesec", "giếc"),
        ("uwmf", "ừm"),
        ("hufmw", "hừm"),
        ("hufwm", "hừm"),
        ("humfw", "hừm"),
        ("humwf", "hừm"),
        ("huwfm", "hừm"),
        ("huwmf", "hừm"),
        ("ngufmw", "ngừm"),
        ("ngumwf", "ngừm"),
        ("nguwmf", "ngừm"),
    ] {
        assert_eq!(
            crate::support::app_text(InputMethod::Telex, &format!("{keys} ")),
            format!("{expected} "),
            "{keys}"
        );
    }
}

#[test]
fn a_horned_ong_outside_place_names_restores_on_the_boundary() {
    // A `w` typed before the `o` horns it, but `ơng` is a place-name rhyme, so
    // `kơng` is no word and Space gives the keys back (#711).
    for keys in ["kwong", "kowng", "twong", "cwong"] {
        assert_eq!(
            crate::support::app_text(InputMethod::Telex, &format!("{keys} ")),
            format!("{keys} "),
            "{keys}"
        );
    }
}

#[test]
fn telex_eager_restore_step() {
    // "tẽ" is still valid Vietnamese, so it composes; the closing "t" makes
    // "tẽt" a dead end, restoring "text" the instant it is typed — no boundary.
    let mut engine = Engine::new();
    for key in "tex".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "tẽ");

    let closing = engine.process_char('t');
    assert_eq!(closing.action, Action::Send);
    assert_eq!(engine.buffer(), "text");
    assert_eq!(engine.keys(), "text");
}

#[test]
fn telex_misordered_consonant_restores_eagerly() {
    // Read order-blind, `cnó` looks like onset `c` + rhyme `ón`. The `n` sits
    // before the vowel, though, so the tone key makes it a dead end and the raw
    // keystrokes come back at once — and stay through the boundary.
    let mut engine = Engine::new();
    for key in "cno".chars() {
        engine.process_char(key);
    }
    engine.process_char('s');
    assert_eq!(engine.buffer(), "cnos");
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, "cnos "),
        "cnos "
    );
}

#[test]
fn telex_mas_space_no_restore() {
    let mut engine = Engine::new();
    engine.process_char('m');
    engine.process_char('a');
    engine.process_char('s');
    let space = engine.process_char(' ');
    assert_eq!(space.action, Action::None);
    assert!(space.output.is_empty());
}
