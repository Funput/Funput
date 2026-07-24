use super::*;

#[test]
fn pair_accepts_tone_but_not_shape() {
    assert_eq!(uo_pair_in_vowel_cluster("truòng"), Some((2, 3)));
    assert_eq!(uo_pair_in_vowel_cluster("trưòng"), None);
}

#[test]
fn applies_and_preserves_tone() {
    assert_eq!(apply_uo_compound("truòng").as_deref(), Some("trường"));
    assert_eq!(apply_uo_compound("quói").as_deref(), Some("quới"));
}

#[test]
fn normalizes_both_intermediate_orders() {
    assert_eq!(
        complete_uo_horn_for_continuation("truơ", 'n').as_deref(),
        Some("trươn")
    );
    assert_eq!(
        continuation::complete_horned_uo_for_continuation("trưo", 'n').as_deref(),
        Some("trươn")
    );
    assert_eq!(normalize_horned_uo_open("thưo").as_deref(), Some("thuơ"));
}
