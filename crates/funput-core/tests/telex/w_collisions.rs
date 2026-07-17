use funput_core::InputMethod;

fn type_keys(keys: &str) -> String {
    crate::support::type_keys(InputMethod::Telex, keys)
}

#[test]
fn snapshots_leading_w_and_latin_collisions() {
    // V2 target: leading w remains literal; post-onset swan becomes Vietnamese
    // and relies on engine flip as the explicit Latin escape.
    for literal in ["was", "wow", "win"] {
        assert_eq!(type_keys(literal), literal);
    }
    assert_eq!(type_keys("swift"), "swift");
    assert_eq!(type_keys("swan"), "săn");
}

#[test]
fn snapshots_repeated_pending_w_and_boundaries() {
    assert_eq!(type_keys("lwwa"), "lwwa");
    assert_eq!(
        crate::support::type_words(InputMethod::Telex, "lw a"),
        "lw a"
    );
}
