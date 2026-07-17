use funput_core::InputMethod;
use funput_engine::{Action, Engine};

#[test]
fn resolves_with_minimal_diff_and_preserves_raw_keys() {
    let (buffer, results) = crate::support::type_keys(InputMethod::Telex, "lwa");
    assert_eq!(buffer, "lă");
    assert_eq!(results[2].action, Action::Send);
    assert_eq!(results[2].backspace, 1);
    assert_eq!(results[2].output, "ă");

    let mut engine = Engine::new();
    for key in "swan".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "săn");
    assert_eq!(engine.keys(), "swan");
}

#[test]
fn flip_is_the_sticky_latin_escape() {
    let mut engine = Engine::new();
    for key in "swan".chars() {
        engine.process_char(key);
    }
    engine.flip_composing();
    assert_eq!(engine.buffer(), "swan");
    let boundary = engine.process_char(' ');
    assert_eq!(boundary.action, Action::None);
    assert!(engine.buffer().is_empty());
}

#[test]
fn literal_and_boundary_cases_do_not_leak() {
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, "was wow win swift "),
        "was wow win swift "
    );
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, "lwwa "),
        "lwwa "
    );
    assert_eq!(
        crate::support::app_text(InputMethod::Telex, "lw a "),
        "lw a "
    );
}

fn insert_w(base: &str, boundary: usize) -> String {
    let offset = base
        .char_indices()
        .nth(boundary)
        .map_or(base.len(), |pair| pair.0);
    format!("{}w{}", &base[..offset], &base[offset..])
}

#[test]
fn compound_permutations_converge_through_engine() {
    for (base, expected) in [
        ("truongf", "trường"),
        ("nguoif", "người"),
        ("mua", "mưa"),
        ("uu", "ưu"),
        ("thuor", "thuở"),
        ("quois", "quới"),
    ] {
        for boundary in 1..=base.chars().count() {
            let keys = insert_w(base, boundary);
            assert_eq!(
                crate::support::app_text(InputMethod::Telex, &keys),
                expected,
                "{keys}"
            );
        }
    }
}

#[test]
fn compound_resolution_preserves_raw_keys() {
    let mut engine = Engine::new();
    for key in "trwuongf".chars() {
        engine.process_char(key);
    }
    assert_eq!(engine.buffer(), "trường");
    assert_eq!(engine.keys(), "trwuongf");
}
