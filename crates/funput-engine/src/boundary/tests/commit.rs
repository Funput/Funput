use super::*;
use crate::boundary::{is_word_boundary, on_word_boundary};
use crate::result::Action;

#[test]
fn boundary_character_inventory() {
    for key in [' ', '\n', '\t', ',', '.', '!', '?', ')', '-', '"'] {
        assert!(is_word_boundary(funput_core::InputMethod::Telex, key));
    }
    for key in ['a', 'z', 'A', '1', '9'] {
        assert!(!is_word_boundary(funput_core::InputMethod::Telex, key));
    }
    assert!(!is_word_boundary(
        funput_core::InputMethod::TelexAdvanced,
        '['
    ));
    assert!(!is_word_boundary(
        funput_core::InputMethod::TelexAdvanced,
        ']'
    ));
}

#[test]
fn boundary_always_clears_session() {
    let mut value = session(InputMethod::Telex, "á", "as");
    on_word_boundary(&mut value, ' ');
    assert!(value.buffer.is_empty());
    assert!(value.keys.is_empty());
}

#[test]
fn shortcut_wins_and_counts_the_displayed_buffer() {
    let mut value = session(InputMethod::Telex, "vn", "vn");
    value.shortcuts.insert("vn".into(), "Việt Nam".into());
    let result = on_word_boundary(&mut value, ' ');
    assert_eq!(result.action, Action::Send);
    assert_eq!(result.backspace, 2);
    assert_eq!(result.output, "Việt Nam ");

    let mut shaped = session(InputMethod::Telex, "á", "as");
    shaped.shortcuts.insert("as".into(), "address".into());
    let result = on_word_boundary(&mut shaped, ' ');
    assert_eq!(result.backspace, 1);
    assert_eq!(result.output, "address ");
}

#[test]
fn shortcuts_are_case_sensitive_and_keep_punctuation() {
    let mut upper = session(InputMethod::Telex, "VN", "VN");
    upper.shortcuts.insert("vn".into(), "Việt Nam".into());
    assert_eq!(on_word_boundary(&mut upper, ' ').action, Action::None);

    let mut punctuated = session(InputMethod::Telex, "kg", "kg");
    punctuated.shortcuts.insert("kg".into(), "không".into());
    assert_eq!(on_word_boundary(&mut punctuated, ',').output, "không,");
}
