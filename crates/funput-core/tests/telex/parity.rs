use funput_core::InputMethod;

#[test]
fn telex_matches_vni_output() {
    for case in crate::parity_data::CASES {
        let telex = crate::support::type_keys(InputMethod::Telex, case.telex_keys);
        let vni = crate::support::type_keys(InputMethod::Vni, case.vni_keys);
        assert_eq!(telex, vni, "{}: telex vs vni output", case.label);
    }
}

#[test]
fn telex_word_parity() {
    for case in crate::parity_data::WORD_CASES {
        let telex = crate::support::type_words(InputMethod::Telex, case.telex_words);
        let vni = crate::support::type_words(InputMethod::Vni, case.vni_words);
        assert_eq!(telex, vni, "{}: telex vs vni words", case.label);
        assert_eq!(telex, case.output, "{}: telex output", case.label);
    }
}
