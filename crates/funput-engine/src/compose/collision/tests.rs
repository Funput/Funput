use funput_core::{InputMethod, telex_defers_w};

use super::*;
use crate::compose::RestoreOverride;
use crate::compose::pipeline;
use crate::compose::sorted_lines::assert_well_formed;

/// Feed `keys` the way `Engine::compose_key` does: raw key first, then the pipeline.
fn typed(method: InputMethod, keys: &str) -> Session {
    let mut session = Session::new();
    session.config.method = method;
    for key in keys.chars() {
        session.push_key(key);
        pipeline::process(&mut session, key, key, false);
    }
    session
}

#[test]
fn every_shipped_list_is_well_formed() {
    for class in Collision::ALL {
        assert_well_formed(class.words());
    }
}

/// `include_str!` needs a literal path, so this ties each embedded list to the
/// directory its [`Collision::name`] names — the one the generator writes.
#[test]
fn every_class_embeds_the_list_its_name_points_at() {
    for class in Collision::ALL {
        let path = format!(
            "{}/data/collision/{}/words.txt",
            env!("CARGO_MANIFEST_DIR"),
            class.name()
        );
        let on_disk = std::fs::read_to_string(&path).expect("the class has a list");
        assert_eq!(class.words().text(), on_disk, "{path}");
    }
}

#[test]
fn every_deferred_w_entry_defers_its_w() {
    for word in Collision::DeferredW.words().lines() {
        assert!(telex_defers_w(word), "{word:?} holds no deferred w");
    }
}

#[test]
fn matches_a_listed_word_in_any_case_with_digits_glued_on() {
    let telex = Session::new().config;
    for keys in ["two", "Two", "TWO", "two2", "swan", "swarms"] {
        assert_eq!(
            Collision::matching(&telex, keys),
            Some(Collision::DeferredW),
            "{keys}"
        );
    }
    // Vietnamese typed with a deferred `w`, and the canonical `tow`, are not listed.
    for keys in ["tow", "cwon", "nwux", "twin", "2", "", "tơ"] {
        assert_eq!(Collision::matching(&telex, keys), None, "{keys}");
    }
}

#[test]
fn only_plain_telex_defers_a_w() {
    for method in [InputMethod::TelexAdvanced, InputMethod::Vni] {
        let mut config = Session::new().config;
        config.method = method;
        assert_eq!(Collision::matching(&config, "two"), None, "{method:?}");
    }
}

#[test]
fn restores_a_listed_word_the_rule_shaped() {
    let session = typed(InputMethod::Telex, "two");
    assert_eq!(session.buffer, "tơ");
    assert!(restorable(&session));
}

#[test]
fn rides_on_english_restore_and_its_own_switch() {
    let mut session = typed(InputMethod::Telex, "two");
    session.config.restore_collisions = false;
    assert!(!restorable(&session));
    session.config.restore_collisions = true;
    session.config.smart_restore = false;
    assert!(!restorable(&session));
}

#[test]
fn a_manual_flip_outranks_the_list() {
    for pinned in [RestoreOverride::ForceRaw, RestoreOverride::ForceVietnamese] {
        let mut session = typed(InputMethod::Telex, "two");
        session.restore_override = Some(pinned);
        assert!(!restorable(&session), "{pinned:?}");
    }
}

#[test]
fn a_word_shown_as_typed_has_nothing_to_restore() {
    let mut session = typed(InputMethod::Telex, "two");
    session.buffer.clone_from(&session.keys);
    assert!(!restorable(&session));
}
