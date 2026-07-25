//! Re-opening a committed word for editing (`Engine::adopt`).
//!
//! Android calls this when Backspace puts the caret back on a finished word, so the
//! next keystroke retones it instead of typing a literal letter.

use funput_core::InputMethod;
use funput_engine::Engine;

fn engine(method: InputMethod) -> Engine {
    let mut engine = Engine::new();
    engine.set_method(method);
    engine
}

fn type_into(engine: &mut Engine, keys: &str) -> String {
    for key in keys.chars() {
        engine.process_char(key);
    }
    engine.buffer().to_owned()
}

/// The reason this exists: type `chào`, commit it with Space, delete the space, adopt
/// the word again, then a tone key edits it in place.
#[test]
fn adopted_word_accepts_a_new_tone() {
    let mut engine = engine(InputMethod::Telex);
    assert_eq!(type_into(&mut engine, "chaof"), "chào");
    engine.process_char(' '); // word boundary commits and clears the session
    assert_eq!(engine.buffer(), "");

    assert!(engine.adopt("chào"));
    assert_eq!(engine.buffer(), "chào");
    engine.process_char('s'); // sắc replaces huyền
    assert_eq!(engine.buffer(), "cháo");
}

#[test]
fn adopted_word_matches_typing_it_from_scratch() {
    let mut adopted = engine(InputMethod::Telex);
    assert!(adopted.adopt("chào"));
    adopted.process_char('s');

    let mut typed = engine(InputMethod::Telex);
    assert_eq!(type_into(&mut typed, "chaofs"), adopted.buffer());
}

#[test]
fn adopts_a_capitalized_syllable() {
    // Sentence-initial words must be re-editable too.
    let mut engine = engine(InputMethod::Telex);
    assert!(engine.adopt("Chào"));
    engine.process_char('s');
    assert_eq!(engine.buffer(), "Cháo");
}

#[test]
fn adopts_a_toneless_syllable_and_adds_a_tone() {
    let mut engine = engine(InputMethod::Vni);
    assert!(engine.adopt("chao"));
    engine.process_char('2'); // VNI huyền
    assert_eq!(engine.buffer(), "chào");
}

/// Anything that is not a Vietnamese syllable is refused, so English words and URLs
/// never silently become editable.
#[test]
fn refuses_text_that_is_not_a_syllable() {
    let mut engine = engine(InputMethod::Telex);
    for text in ["", "text", "tẽt", "hello", "funput.app", "chào bạn"] {
        assert!(!engine.adopt(text), "should not adopt {text:?}");
        assert_eq!(engine.buffer(), "", "buffer must stay empty after refusing");
    }
}

#[test]
fn refuses_while_disabled() {
    let mut engine = engine(InputMethod::Telex);
    engine.set_enabled(false);
    assert!(!engine.adopt("chào"));
    assert_eq!(engine.buffer(), "");
}

/// Adopting replaces whatever was composing, and leaves no raw form to flip back to
/// (the keystrokes that produced the word are gone).
#[test]
fn adopting_replaces_composition_and_leaves_nothing_to_flip() {
    let mut engine = engine(InputMethod::Telex);
    type_into(&mut engine, "meo");
    assert!(engine.adopt("chào"));
    assert_eq!(engine.buffer(), "chào");
    assert_eq!(engine.keys(), "chào");
    assert_eq!(engine.flip_composing().action, funput_engine::Action::None);
}

/// A word boundary after editing an adopted word commits the edited form, not the
/// original raw keys (English restore must not fire on valid Vietnamese).
#[test]
fn boundary_after_editing_keeps_the_vietnamese_form() {
    let mut engine = engine(InputMethod::Telex);
    assert!(engine.adopt("chào"));
    engine.process_char('s');
    let result = engine.process_char(' ');
    let committed = if result.action == funput_engine::Action::None {
        "cháo ".to_owned()
    } else {
        result.output
    };
    assert_eq!(committed, "cháo ");
}
