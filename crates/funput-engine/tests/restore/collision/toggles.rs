use funput_core::InputMethod;
use funput_engine::Engine;

use super::engine;
use crate::support::{Document, app_text_in};

/// Type `script` into a document, where `^` presses the flip hotkey.
fn typed(engine: &mut Engine, script: &str) -> String {
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

#[test]
fn smart_restore_off_turns_it_off() {
    let mut on = engine(InputMethod::Telex, true);
    on.update_config(|config| config.smart_restore = false);
    assert_eq!(app_text_in(&mut on, "two "), "tơ ");
}

#[test]
fn eager_restore_off_still_restores_at_the_boundary() {
    let mut on = engine(InputMethod::Telex, true);
    on.update_config(|config| config.eager_restore = false);
    assert_eq!(app_text_in(&mut on, "two swan "), "two swan ");
}

/// Flip once shows the keys; flip again pins the Vietnamese, which the list must
/// not take away: that is how a deferred-`w` typist keeps `tơ`.
#[test]
fn a_manual_flip_outranks_the_list() {
    assert_eq!(
        typed(&mut engine(InputMethod::Telex, true), "two^ "),
        "two "
    );
    assert_eq!(
        typed(&mut engine(InputMethod::Telex, true), "two^^ "),
        "tơ "
    );
}

#[test]
fn a_shortcut_on_the_typed_keys_wins() {
    let mut on = engine(InputMethod::Telex, true);
    on.add_shortcut("two", "hai");
    assert_eq!(app_text_in(&mut on, "two "), "hai ");
}

#[test]
fn a_restored_word_parks_no_typo_correction() {
    let mut on = engine(InputMethod::Telex, true);
    on.update_config(|config| config.typo_correction = true);
    app_text_in(&mut on, "two ");
    assert!(!on.has_pending_correction());
}
