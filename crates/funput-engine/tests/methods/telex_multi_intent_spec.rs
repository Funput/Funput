use funput_core::InputMethod;

#[test]
fn pending_w_then_stroke_converges() {
    for (keys, output) in [
        ("dwduocj", "được"),
        ("dwduongf", "đường"),
        ("dwdon", "đơn"),
        ("dwdoif", "đời"),
    ] {
        assert_eq!(crate::support::app_text(InputMethod::Telex, keys), output);
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
