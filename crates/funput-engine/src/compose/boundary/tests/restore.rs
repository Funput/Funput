use super::*;
use crate::Action;
use crate::compose::RestoreOverride;
use crate::compose::boundary::{on_word_boundary, should_restore};
use crate::model::Session;

#[test]
fn restore_decision_uses_completed_structure() {
    assert!(should_restore(&session(InputMethod::Telex, "ábc", "absc")));
    assert!(!should_restore(&session(InputMethod::Telex, "má", "mas")));
    assert!(!should_restore(&session(
        InputMethod::Telex,
        "text",
        "text"
    )));
    assert!(!should_restore(&Session::new()));
}

#[test]
fn explicit_vietnamese_modifiers_remain_pinned() {
    assert!(!should_restore(&session(InputMethod::Vni, "đc", "d9c")));
    assert!(!should_restore(&session(InputMethod::Telex, "GĐ", "GDD")));
}

#[test]
fn unresolved_multi_intent_is_not_pinned_by_stroke() {
    assert!(should_restore(&session(InputMethod::Telex, "đw", "dwd")));
}

#[test]
fn force_vietnamese_suppresses_restore() {
    let mut value = session(InputMethod::Telex, "cải", "caix");
    value.restore_override = Some(RestoreOverride::ForceVietnamese);
    assert!(!should_restore(&value));
}

#[test]
fn boundary_restores_raw_keys_and_clears_session() {
    let mut value = session(InputMethod::Telex, "ábc", "absc");
    let result = on_word_boundary(&mut value, ' ');
    assert_eq!(result.action, Action::Send);
    assert_eq!(result.backspace, 3);
    assert_eq!(result.output, "absc ");
    assert!(value.buffer.is_empty());
    assert!(value.keys.is_empty());
}

#[test]
fn valid_vietnamese_commits_without_restore() {
    let mut value = session(InputMethod::Telex, "má", "mas");
    let result = on_word_boundary(&mut value, ' ');
    assert_eq!(result.action, Action::None);
    assert!(value.buffer.is_empty());
    assert!(value.keys.is_empty());
}
