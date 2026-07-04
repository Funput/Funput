
use funput_core::InputMethod;

#[test]
fn telex_fixture_cases() {
    for case in crate::cases::CASES {
        assert_eq!(
            crate::support::type_keys(InputMethod::Telex, case.keys),
            case.output,
            "{}",
            case.label
        );
    }
}

#[test]
fn telex_fixture_word_cases() {
    for case in crate::cases::WORD_CASES {
        assert_eq!(
            crate::support::type_words(InputMethod::Telex, case.words),
            case.output,
            "{}",
            case.label
        );
    }
}

#[test]
fn telex_full_regression() {
    telex_fixture_cases();
    telex_fixture_word_cases();
}
