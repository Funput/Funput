use funput_core::InputMethod;

#[test]
fn vni_fixture_cases() {
    for case in crate::cases::CASES {
        assert_eq!(
            crate::support::type_keys(InputMethod::Vni, case.keys),
            case.output,
            "{}",
            case.label
        );
    }
}

#[test]
fn vni_fixture_word_cases() {
    for case in crate::cases::WORD_CASES {
        assert_eq!(
            crate::support::type_words(InputMethod::Vni, case.words),
            case.output,
            "{}",
            case.label
        );
    }
}

#[test]
fn vni_full_regression() {
    vni_fixture_cases();
    vni_fixture_word_cases();
}
