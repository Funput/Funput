//! Text-expansion (gõ tắt) integration tests at the [`Engine`] level.
//!
//! A trigger matches the raw keystrokes since the last word boundary and expands at
//! the boundary — taking priority over English restore. Matching is smart-case: a
//! trigger typed as lowercase, Title Case, or UPPERCASE all resolve to the same
//! entry, and the expansion is re-cased to match. Keystrokes with a mixed case that
//! doesn't fit one of those three patterns fall back to an exact match only. See
//! `src/compose/boundary/mod.rs` for the matching logic.

use funput_core::InputMethod;
use funput_engine::{Action, Engine};

/// Drive `keys` through an engine seeded with `shortcuts`, reconstructing the
/// resulting app text from the inject stream (None → append, Send → delete + append).
fn app_text_with_shortcuts(method: InputMethod, shortcuts: &[(&str, &str)], keys: &str) -> String {
    let mut engine = Engine::new();
    engine.set_method(method);
    for (trigger, expansion) in shortcuts {
        engine.add_shortcut(*trigger, *expansion);
    }
    let mut app = String::new();
    for key in keys.chars() {
        let r = engine.process_char(key);
        match r.action {
            Action::None => app.push(key),
            Action::Send => {
                for _ in 0..r.backspace {
                    app.pop();
                }
                app.push_str(&r.output);
            }
            Action::Restore => unreachable!("Restore not implemented yet"),
        }
    }
    app
}

#[test]
fn expands_trigger_on_space_telex() {
    let text = app_text_with_shortcuts(InputMethod::Telex, &[("vn", "Việt Nam")], "vn ");
    assert_eq!(text, "Việt Nam ");
}

#[test]
fn expands_trigger_on_space_vni() {
    let text = app_text_with_shortcuts(InputMethod::Vni, &[("vn", "Việt Nam")], "vn ");
    assert_eq!(text, "Việt Nam ");
}

#[test]
fn punctuation_boundary_is_kept() {
    let text = app_text_with_shortcuts(InputMethod::Telex, &[("kg", "không")], "kg,");
    assert_eq!(text, "không,");
}

#[test]
fn expansion_wins_over_english_restore() {
    // `card` would normally restore to its raw keys at the boundary; the trigger
    // takes priority and expands instead.
    let text = app_text_with_shortcuts(InputMethod::Telex, &[("card", "credit card")], "card ");
    assert_eq!(text, "credit card ");
}

#[test]
fn uppercase_trigger_expands_to_uppercase() {
    let text = app_text_with_shortcuts(InputMethod::Telex, &[("vn", "việt nam")], "VN ");
    assert_eq!(text, "VIỆT NAM ");
}

#[test]
fn title_case_trigger_expands_to_title_case() {
    let text = app_text_with_shortcuts(InputMethod::Telex, &[("vn", "việt nam")], "Vn ");
    assert_eq!(text, "Việt Nam ");
}

#[test]
fn title_case_capitalizes_every_word_in_a_multi_word_expansion() {
    let text = app_text_with_shortcuts(InputMethod::Telex, &[("kg", "không có gì")], "Kg ");
    assert_eq!(text, "Không Có Gì ");
}

#[test]
fn single_uppercase_letter_trigger_capitalizes_rather_than_shouts() {
    // A lone capital reads as "capitalize" (Title Case), not "shout the whole
    // expansion in uppercase" — that requires 2+ uppercase letters (`VN`, not `V`).
    let text = app_text_with_shortcuts(InputMethod::Telex, &[("v", "việt nam")], "V ");
    assert_eq!(text, "Việt Nam ");
}

#[test]
fn mixed_case_trigger_without_clean_pattern_falls_back_to_exact_match() {
    // `vNa` doesn't fit lowercase/Title/UPPERCASE, so no smart-case lookup applies;
    // since no exact `vNa` entry exists either, it falls through unchanged.
    let text = app_text_with_shortcuts(InputMethod::Telex, &[("vn", "việt nam")], "vNa ");
    assert_eq!(text, "vNa ");
}

#[test]
fn deliberately_mixed_case_trigger_still_matches_exactly() {
    // A trigger defined with intentional mixed case (e.g. a brand name) keeps working
    // via the exact-match fallback.
    let text = app_text_with_shortcuts(InputMethod::Telex, &[("iOS", "iPhone OS")], "iOS ");
    assert_eq!(text, "iPhone OS ");
}

#[test]
fn shortcut_only_at_boundary_not_mid_word() {
    // `vna` (no boundary after `vn`) must not expand.
    let text = app_text_with_shortcuts(InputMethod::Telex, &[("vn", "Việt Nam")], "vna ");
    assert_ne!(text, "Việt Nama ");
}

#[test]
fn undefined_trigger_leaves_normal_behavior_unchanged() {
    // No shortcuts at all: a valid Vietnamese word still composes normally.
    let text = app_text_with_shortcuts(InputMethod::Telex, &[], "mas ");
    assert_eq!(text, "má ");
}

#[test]
fn remove_and_clear_shortcuts() {
    let mut engine = Engine::new();
    engine.add_shortcut("vn", "Việt Nam");
    engine.add_shortcut("kg", "không");
    assert_eq!(engine.shortcuts().len(), 2);

    engine.remove_shortcut("vn");
    assert!(!engine.shortcuts().contains_key("vn"));
    assert!(engine.shortcuts().contains_key("kg"));

    engine.clear_shortcuts();
    assert!(engine.shortcuts().is_empty());
}

#[test]
fn empty_trigger_is_ignored() {
    let mut engine = Engine::new();
    engine.add_shortcut("", "nothing");
    assert!(engine.shortcuts().is_empty());
}

#[test]
fn re_adding_trigger_overwrites() {
    let mut engine = Engine::new();
    engine.add_shortcut("vn", "Việt Nam");
    engine.add_shortcut("vn", "Vietnam");
    assert_eq!(
        engine.shortcuts().get("vn").map(String::as_str),
        Some("Vietnam")
    );
}

/// Drive `keys` with gõ tắt switched off, otherwise identical to
/// [`app_text_with_shortcuts`].
fn app_text_with_shortcuts_off(shortcuts: &[(&str, &str)], keys: &str) -> String {
    let mut engine = Engine::new();
    engine.update_config(|config| config.shortcuts_enabled = false);
    for (trigger, expansion) in shortcuts {
        engine.add_shortcut(*trigger, *expansion);
    }
    let mut app = String::new();
    for key in keys.chars() {
        let r = engine.process_char(key);
        match r.action {
            Action::None => app.push(key),
            Action::Send => {
                for _ in 0..r.backspace {
                    app.pop();
                }
                app.push_str(&r.output);
            }
            Action::Restore => unreachable!("Restore not implemented yet"),
        }
    }
    app
}

#[test]
fn the_switch_stops_expansion_without_touching_the_table() {
    let text = app_text_with_shortcuts_off(&[("vn", "Việt Nam")], "vn ");
    assert_eq!(text, "vn ", "the trigger must stay as typed");
}

#[test]
fn a_word_that_is_not_a_trigger_still_composes_with_the_switch_off() {
    // Turning gõ tắt off must not disturb ordinary Vietnamese composition.
    let text = app_text_with_shortcuts_off(&[("vn", "Việt Nam")], "chaof ");
    assert_eq!(text, "chào ");
}

#[test]
fn the_table_survives_the_switch_so_flipping_back_costs_nothing() {
    let mut engine = Engine::new();
    engine.add_shortcut("vn", "Việt Nam");
    engine.update_config(|config| config.shortcuts_enabled = false);
    assert_eq!(
        engine.shortcuts().get("vn").map(String::as_str),
        Some("Việt Nam"),
        "the rows must outlive the switch"
    );
    engine.update_config(|config| config.shortcuts_enabled = true);
    for key in "vn ".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.shortcuts().len(), 1);
}
