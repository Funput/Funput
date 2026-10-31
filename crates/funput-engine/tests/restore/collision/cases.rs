use funput_core::InputMethod;

use super::{DEFERRED_W, shown};

#[test]
fn issue_661_two_comes_back_as_typed() {
    assert_eq!(shown(InputMethod::Telex, false, "two "), "tơ ");
    assert_eq!(shown(InputMethod::Telex, true, "two "), "two ");
    assert_eq!(
        shown(InputMethod::Telex, true, "Two, TWO. two2 "),
        "Two, TWO. two2 "
    );
}

#[test]
fn every_listed_word_comes_back_as_typed() {
    for word in DEFERRED_W.lines() {
        let typed = format!("{word} ");
        assert_ne!(
            shown(InputMethod::Telex, false, &typed),
            typed,
            "{word} collides"
        );
        assert_eq!(shown(InputMethod::Telex, true, &typed), typed, "{word}");
    }
}

#[test]
fn vietnamese_through_the_same_rule_stays_vietnamese() {
    assert_eq!(
        shown(
            InputMethod::Telex,
            true,
            "cwon nwux tow sawn trwuongf twin "
        ),
        "cơn nữ tơ săn trường twin "
    );
}

/// The other methods never defer a `w`, so the feature changes nothing there.
#[test]
fn other_methods_type_as_before() {
    let words: String = DEFERRED_W.lines().map(|word| format!("{word} ")).collect();
    for method in [InputMethod::TelexAdvanced, InputMethod::Vni] {
        assert_eq!(
            shown(method, true, &words),
            shown(method, false, &words),
            "{method:?}"
        );
    }
}
