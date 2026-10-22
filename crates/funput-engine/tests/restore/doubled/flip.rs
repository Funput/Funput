//! Flip after a revert: the raw form it shows is the keys as typed, so the key a
//! doubled tone key swallowed is always one press away, listed word or not.

use funput_core::InputMethod;
use funput_engine::Engine;

use super::engine;
use crate::support::Document;

/// Type `script` into a document, where `^` presses the flip hotkey.
fn shown(engine: &mut Engine, script: &str) -> String {
    let mut document = Document::new();
    for key in script.chars() {
        if key == '^' {
            document.edited(&engine.flip_composing());
        } else {
            let result = engine.process_char(key);
            document.typed(key, &result);
        }
    }
    document.text().to_owned()
}

fn lazy() -> Engine {
    let mut lazy = engine(InputMethod::Telex, true);
    lazy.update_config(|config| config.eager_restore = false);
    lazy
}

#[test]
fn flip_brings_back_the_swallowed_key() {
    let mut on = engine(InputMethod::Telex, true);
    assert_eq!(shown(&mut on, "pass^"), "pass");
    // `keys` follows, so the boundary sees the word already shown as typed.
    assert_eq!(on.keys(), "pass");
    assert_eq!(
        shown(&mut engine(InputMethod::Telex, true), "pass^word "),
        "password "
    );
}

#[test]
fn flip_to_raw_keeps_every_key_with_eager_restore_off() {
    // Waiting for the boundary, the word shows its Vietnamese form until the flip.
    assert_eq!(shown(&mut lazy(), "passwor^"), "passwor");
    assert_eq!(shown(&mut lazy(), "passwor^d "), "password ");
    // Flipped right before the boundary, the raw form is what gets committed.
    assert_eq!(shown(&mut lazy(), "passwor^ "), "passwor ");
}

#[test]
fn flip_reaches_a_word_the_table_does_not_list() {
    // `passwd` is no English word, so only the flip can bring its second `s` back.
    assert_eq!(shown(&mut lazy(), "passwd "), "paswd ");
    assert_eq!(shown(&mut lazy(), "passwd^"), "passwd");
    assert_eq!(shown(&mut lazy(), "passwd^ "), "passwd ");
}

#[test]
fn flipping_twice_keeps_the_shortened_word() {
    // From the literal the flip goes to the Vietnamese form, here the shortened `pas`,
    // and pins it, so the boundary leaves it as it is.
    let mut on = engine(InputMethod::Telex, true);
    assert_eq!(shown(&mut on, "pass^^ "), "pas ");
}

#[test]
fn switched_off_the_flip_is_what_it_was() {
    let mut off = engine(InputMethod::Telex, false);
    // `vn_form` and `keys` are both `pas`, so there is nothing to flip to.
    assert_eq!(shown(&mut off, "pass^ "), "pas ");
}
