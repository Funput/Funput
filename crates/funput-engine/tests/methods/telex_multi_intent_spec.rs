use funput_core::InputMethod;

#[test]
fn snapshots_current_pending_w_then_stroke_gap() {
    for literal in ["dwduocj", "dwduongf", "dwdon", "dwdoif"] {
        assert_eq!(
            crate::support::app_text(InputMethod::Telex, literal),
            literal
        );
    }
}

#[test]
fn latin_collisions_keep_current_engine_behavior() {
    for (keys, output) in [
        ("dad ", "đa "),
        ("did ", "đi "),
        ("dead ", "dead "),
        ("droid ", "droid "),
        ("dwdead ", "dwdead "),
        ("dwswift ", "dwswift "),
    ] {
        assert_eq!(crate::support::app_text(InputMethod::Telex, keys), output);
    }
}
