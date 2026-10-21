use funput_core::InputMethod;
use funput_engine::Engine;

use super::engine;
use crate::support::app_text_in;

/// The same keys through an engine with the feature off, set up by `prepare`.
fn without_feature(keys: &str, prepare: impl Fn(&mut Engine)) -> String {
    let mut engine = engine(InputMethod::Telex, false);
    prepare(&mut engine);
    app_text_in(&mut engine, keys)
}

fn with_feature(keys: &str, prepare: impl Fn(&mut Engine)) -> String {
    let mut engine = engine(InputMethod::Telex, true);
    prepare(&mut engine);
    app_text_in(&mut engine, keys)
}

#[test]
fn switched_off_it_types_as_before() {
    assert_eq!(without_feature("password pass ", |_| {}), "pasword pas ");
}

#[test]
fn smart_restore_off_turns_it_off() {
    let smart_off = |engine: &mut Engine| engine.update_config(|c| c.smart_restore = false);
    assert_eq!(
        with_feature("password correct ", smart_off),
        without_feature("password correct ", smart_off)
    );
}

#[test]
fn eager_restore_off_still_restores_at_the_boundary() {
    let eager_off = |engine: &mut Engine| engine.update_config(|c| c.eager_restore = false);
    assert_eq!(with_feature("password ", eager_off), "password ");
}

#[test]
fn a_flip_before_the_doubled_key_wins() {
    let mut on = engine(InputMethod::Telex, true);
    let mut off = engine(InputMethod::Telex, false);
    for engine in [&mut on, &mut off] {
        app_text_in(engine, "pas");
        engine.flip_composing();
    }
    assert_eq!(
        app_text_in(&mut on, "sword "),
        app_text_in(&mut off, "sword ")
    );
}

#[test]
fn a_shortcut_on_the_shortened_keys_wins() {
    let shortcut = |engine: &mut Engine| engine.add_shortcut("pas", "Pasteur");
    assert_eq!(with_feature("pass ", shortcut), "Pasteur ");
    assert_eq!(without_feature("pass ", shortcut), "Pasteur ");
}
