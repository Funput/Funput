use super::*;
use crate::Action;
use crate::compose::boundary::{is_word_boundary, on_word_boundary};

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
fn uppercase_trigger_expands_to_uppercase_expansion() {
    let mut upper = session(InputMethod::Telex, "VN", "VN");
    upper.shortcuts.insert("vn".into(), "việt nam".into());
    let result = on_word_boundary(&mut upper, ' ');
    assert_eq!(result.action, Action::Send);
    assert_eq!(result.output, "VIỆT NAM ");
}

#[test]
fn mixed_case_trigger_without_clean_pattern_does_not_expand() {
    // `vNa` doesn't fit lowercase/Title/UPPERCASE, so it isn't smart-case matched —
    // and there's no exact `vNa` entry either.
    let mut mixed = session(InputMethod::Telex, "vNa", "vNa");
    mixed.shortcuts.insert("vn".into(), "Việt Nam".into());
    assert_eq!(on_word_boundary(&mut mixed, ' ').action, Action::None);
}

#[test]
fn shortcuts_keep_punctuation() {
    let mut punctuated = session(InputMethod::Telex, "kg", "kg");
    punctuated.shortcuts.insert("kg".into(), "không".into());
    assert_eq!(on_word_boundary(&mut punctuated, ',').output, "không,");
}
