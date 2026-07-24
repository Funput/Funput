use crate::{InputMethod, ToneStyle, TransformKind, apply, apply_checked};

fn typed(keys: &str, method: InputMethod) -> String {
    keys.chars().fold(String::new(), |buffer, key| {
        apply(&buffer, key, method, ToneStyle::Traditional).text
    })
}

#[test]
fn shared_actions_and_reverts() {
    assert_eq!(typed("a61", InputMethod::Vni), "ấ");
    assert_eq!(typed("a66", InputMethod::Vni), "a6");
    assert_eq!(typed("ass", InputMethod::Telex), "as");
    assert_eq!(typed("dd", InputMethod::Telex), "đ");
    assert_eq!(typed("asz", InputMethod::Telex), "a");
}

#[test]
fn compound_and_tone_order() {
    assert_eq!(typed("tru7o7n2g", InputMethod::Vni), "trường");
    assert_eq!(typed("moiwf", InputMethod::Telex), "mời");
    assert_eq!(typed("asw", InputMethod::Telex), "ắ");
    assert_eq!(typed("nuawx", InputMethod::Telex), "nữa");
    assert_eq!(typed("nuaww", InputMethod::Telex), "nuaw");
}

#[test]
fn spell_check_and_pending() {
    let blocked = apply_checked("tet", 'f', InputMethod::Telex, ToneStyle::Traditional, true);
    assert_eq!(blocked.kind, TransformKind::Pending);
    assert_eq!(blocked.text, "tetf");
    assert_eq!(
        apply("a", 'b', InputMethod::Vni, ToneStyle::Traditional).text,
        "ab"
    );
}

#[test]
fn modern_repositions_open_clusters() {
    let output = "ho2a".chars().fold(String::new(), |buffer, key| {
        apply(&buffer, key, InputMethod::Vni, ToneStyle::Modern).text
    });
    assert_eq!(output, "hoà");
}
