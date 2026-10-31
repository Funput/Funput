use funput_core::InputMethod;

fn type_keys(keys: &str) -> String {
    crate::support::type_keys(InputMethod::Telex, keys)
}

#[test]
fn snapshots_leading_w_and_latin_collisions() {
    // Leading w remains literal; post-onset `swan` composes to `săn` here in the
    // core. The engine gives listed English words like it back at the word
    // boundary (collision restore, #661); Flip stays the escape for the rest.
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
