use super::*;

#[test]
fn apply_shape_basic() {
    assert_eq!(apply_shape('a', VowelShape::Circumflex), Some('â'));
    assert_eq!(apply_shape('o', VowelShape::Horn), Some('ơ'));
    assert_eq!(apply_shape('a', VowelShape::Breve), Some('ă'));
}

#[test]
fn apply_shape_to_vowel_strips_tone() {
    assert_eq!(apply_shape_to_vowel('á', VowelShape::Breve), Some('ă'));
    assert_eq!(apply_shape_to_vowel('â', VowelShape::Circumflex), None);
}

#[test]
fn shape_target_index_picks_receiving_vowel() {
    // `muoi` + circumflex targets the `o` (index 2), not the trailing `i`.
    assert_eq!(shape_target_index("muoi", VowelShape::Circumflex), Some(2));
    assert_eq!(shape_target_index("loi", VowelShape::Circumflex), Some(1));
    assert_eq!(shape_target_index("to", VowelShape::Circumflex), Some(1));
    // No vowel can take the shape.
    assert_eq!(shape_target_index("ly", VowelShape::Circumflex), None);
}

#[test]
fn shape_target_index_horn_on_uu_targets_first_u() {
    // Horn on a bare `uu` run makes `ưu` (cừu, trừu): the first `u` is horned,
    // not the trailing one (which would give the invalid `uư`).
    assert_eq!(shape_target_index("cuu", VowelShape::Horn), Some(1));
    assert_eq!(shape_target_index("truu", VowelShape::Horn), Some(2));
    // A single `u` is unambiguous.
    assert_eq!(shape_target_index("cu", VowelShape::Horn), Some(1));
}

#[test]
fn shaped_vowel_index_finds_existing_shape() {
    assert_eq!(shaped_vowel_index("muôi", VowelShape::Circumflex), Some(2));
    assert_eq!(shaped_vowel_index("muoi", VowelShape::Circumflex), None);
}

#[test]
fn shape_on_vowel_and_strip_shape() {
    for (shaped, shape, base) in [
        ('â', VowelShape::Circumflex, 'a'),
        ('ă', VowelShape::Breve, 'a'),
        ('ê', VowelShape::Circumflex, 'e'),
        ('ô', VowelShape::Circumflex, 'o'),
        ('ơ', VowelShape::Horn, 'o'),
        ('ư', VowelShape::Horn, 'u'),
    ] {
        assert_eq!(shape_on_vowel(shaped), Some(shape));
        assert_eq!(strip_shape(shaped), Some(base));
        assert_eq!(apply_shape(base, shape), Some(shaped));
    }
    assert_eq!(strip_shape('ấ'), Some('á'));
    assert_eq!(shape_on_vowel('ấ'), Some(VowelShape::Circumflex));
}
