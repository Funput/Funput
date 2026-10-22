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

/// A trigger is matched against the keys as typed first, so it fires the same way
/// whether the word was decided mid-word (`office`) or only at its boundary (`pass`).
#[test]
fn a_shortcut_on_the_typed_keys_fires_with_eager_restore_on_or_off() {
    for eager in [true, false] {
        let shortcuts = |engine: &mut Engine| {
            engine.update_config(|c| c.eager_restore = eager);
            engine.add_shortcut("office", "Văn phòng");
            engine.add_shortcut("pass", "mật khẩu");
        };
        assert_eq!(
            with_feature("office ", shortcuts),
            "Văn phòng ",
            "eager {eager}"
        );
        assert_eq!(
            with_feature("pass ", shortcuts),
            "mật khẩu ",
            "eager {eager}"
        );
    }
}

/// With a trigger for each spelling, the keys as typed win: `pass` was typed.
#[test]
fn the_trigger_for_the_typed_keys_beats_the_one_for_the_shortened_keys() {
    let both = |engine: &mut Engine| {
        engine.add_shortcut("pas", "Pasteur");
        engine.add_shortcut("pass", "mật khẩu");
    };
    assert_eq!(with_feature("pass ", both), "mật khẩu ");
    assert_eq!(without_feature("pass ", both), "Pasteur ");
}

/// Alone, a trigger written for the shortened keys still fires: the habit of doubling
/// a key for a plain letter meant `pas`.
#[test]
fn a_shortcut_on_the_shortened_keys_still_fires() {
    let shortcut = |engine: &mut Engine| engine.add_shortcut("pas", "Pasteur");
    assert_eq!(with_feature("pass ", shortcut), "Pasteur ");
    assert_eq!(without_feature("pass ", shortcut), "Pasteur ");
}
