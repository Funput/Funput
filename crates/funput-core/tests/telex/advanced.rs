use funput_core::{InputMethod, ToneStyle, TransformKind, apply, apply_checked};

fn typed(keys: &str) -> String {
    crate::support::type_keys(InputMethod::TelexAdvanced, keys)
}

#[test]
fn full_telex_shortcuts_compose() {
    for (keys, output) in [
        ("w", "ư"),
        ("W", "Ư"),
        ("wf", "ừ"),
        ("ww", "w"),
        ("WW", "W"),
        ("t[", "tư"),
        ("m]", "mơ"),
        ("tr[]ngf", "trường"),
        ("ng[]if", "người"),
        ("wngf", "ừng"),
        ("WWindowws", "Windows"),
    ] {
        assert_eq!(typed(keys), output, "{keys}");
    }
}

#[test]
fn every_tone_and_removal_work_on_shortcuts() {
    for (key, output) in [('s', "ứ"), ('f', "ừ"), ('r', "ử"), ('x', "ữ"), ('j', "ự")] {
        assert_eq!(typed(&format!("w{key}")), output);
    }
    assert_eq!(typed("wfs"), "ứ");
    assert_eq!(typed("wfz"), "ư");
}

#[test]
fn tone_styles_and_existing_telex_converge() {
    for style in [ToneStyle::Traditional, ToneStyle::Modern] {
        let mut buffer = String::new();
        for key in "tr[]ngf".chars() {
            buffer = apply(&buffer, key, InputMethod::TelexAdvanced, style).text;
        }
        assert_eq!(buffer, "trường");
    }
    for keys in ["chana", "trwuongf", "dwduocj", "booong"] {
        assert_eq!(
            typed(keys),
            crate::support::type_keys(InputMethod::Telex, keys)
        );
    }
}

#[test]
fn standard_methods_keep_shortcuts_literal() {
    assert_eq!(crate::support::type_keys(InputMethod::Telex, "w"), "w");
    assert_eq!(crate::support::type_keys(InputMethod::Telex, "t["), "t[");
    assert_eq!(crate::support::type_keys(InputMethod::Vni, "m]"), "m]");
    assert_eq!(typed("{"), "{");
    assert_eq!(typed("}"), "}");
}

#[test]
fn spell_check_falls_back_to_the_raw_shortcut() {
    let result = apply_checked(
        "text",
        '[',
        InputMethod::TelexAdvanced,
        ToneStyle::Traditional,
        true,
    );
    assert_eq!(result.kind, TransformKind::Pending);
    assert_eq!(result.text, "text[");
}
