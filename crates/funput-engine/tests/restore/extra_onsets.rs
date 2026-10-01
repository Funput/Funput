//! The opt-in extra onsets (`zô`, `jờ`, `fải`, `wá`) through the engine: they
//! compose, English restore keeps them, and a word whose rhyme Vietnamese lacks
//! still restores. Off — the default — nothing changes.

use funput_core::{ExtraOnsets, InputMethod, SyllableRules, ToneStyle};
use funput_engine::Engine;

use crate::support::{app_text, app_text_in};

fn engine(method: InputMethod) -> Engine {
    let mut engine = Engine::new();
    engine.set_method(method);
    engine.update_config(|config| {
        config.tone_style = ToneStyle::Traditional;
        config.syllable_rules = SyllableRules::STANDARD.with_extra_onsets(ExtraOnsets::ZFWJ);
    });
    engine
}

/// The app text after typing `keys` with every extra onset admitted.
fn admitted(method: InputMethod, keys: &str) -> String {
    app_text_in(&mut engine(method), keys)
}

#[test]
fn words_survive_the_word_boundary() {
    assert_eq!(
        admitted(InputMethod::Telex, "zoo jowf fair was Juts "),
        "zô jờ fải wá Jút "
    );
    assert_eq!(
        admitted(InputMethod::Vni, "zo6 jo72 fa3i wa1 Ju1t "),
        "zô jờ fải wá Jút "
    );
    // Full Telex keeps `w` → `ư`; doubling it reaches the consonant.
    assert_eq!(
        admitted(InputMethod::TelexAdvanced, "wa wwas zoo "),
        "ưa wá zô "
    );
}

#[test]
fn english_without_a_vietnamese_rhyme_still_restores() {
    for word in [
        "food ", "wood ", "wear ", "zebra ", "from ", "jump ", "file ",
    ] {
        assert_eq!(admitted(InputMethod::Telex, word), word);
    }
}

#[test]
fn eager_restore_judges_by_the_same_rules() {
    let mut engine = engine(InputMethod::Telex);
    for key in "zoo".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "zô");
    // `zôd` can never be Vietnamese, so the raw keys come back on the `d`.
    engine.process_char('d');
    assert_eq!(engine.buffer(), "zood");
}

#[test]
fn english_with_a_vietnamese_rhyme_composes_like_a_native_onset() {
    // The trade-off the switch makes in every Vietnamese IME: these words compose
    // exactly as `vast` → `vát` and `bin10` → `bin` already do after a native onset.
    assert_eq!(
        admitted(InputMethod::Telex, "just for fast "),
        "jút fỏ fát "
    );
    assert_eq!(app_text(InputMethod::Telex, "vast "), "vát ");
    assert_eq!(admitted(InputMethod::Vni, "win10 "), "win ");
    assert_eq!(app_text(InputMethod::Vni, "bin10 "), "bin ");
}

#[test]
fn off_by_default_nothing_changes() {
    for (method, keys) in [
        (InputMethod::Telex, "zoo jowf fair was just fast "),
        (InputMethod::Vni, "zo6 jo72 fa3i wa1 Ju1t win10 "),
    ] {
        assert_eq!(app_text(method, keys), keys);
    }
    assert_eq!(app_text(InputMethod::TelexAdvanced, "wa wwas "), "ưa was ");
}
