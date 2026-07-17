use funput_core::InputMethod;

fn type_keys(keys: &str) -> String {
    crate::support::type_keys(InputMethod::Telex, keys)
}

#[test]
fn delayed_stroke_behavior_is_locked() {
    for (keys, output) in [
        ("daud", "đau"),
        ("dend", "đen"),
        ("doifd", "đòi"),
        ("dad", "đa"),
        ("did", "đi"),
    ] {
        assert_eq!(type_keys(keys), output, "collision changed: {keys}");
    }
}

#[test]
fn invalid_or_repeated_pending_intents_stay_literal() {
    for literal in ["dwdead", "dwswift", "dwdd", "dwwd", "lwwa"] {
        assert_eq!(type_keys(literal), literal);
    }
    assert_eq!(
        crate::support::type_words(InputMethod::Telex, "dwd a"),
        "dwd a"
    );
}
