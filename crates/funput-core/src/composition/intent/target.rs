use crate::unicode::marks::vowel_stem;
use crate::unicode::shapes::{VowelShape, shape_on_vowel, strip_shape};

#[derive(Debug, Clone, Copy)]
pub(super) struct Target {
    pub char_index: usize,
    pub byte_offset: usize,
    pub vowel: char,
}

pub(super) fn rightmost_stem(buffer: &str, stem: char) -> Option<Target> {
    let mut target = None;
    for (char_index, (byte_offset, vowel)) in buffer.char_indices().enumerate() {
        if !vowel.is_alphabetic() {
            target = None;
            continue;
        }
        if !matches!(shape_on_vowel(vowel), None | Some(VowelShape::Circumflex)) {
            continue;
        }
        let plain = strip_shape(vowel).unwrap_or(vowel);
        if vowel_stem(plain).is_some_and(|value| value.eq_ignore_ascii_case(&stem)) {
            target = Some(Target {
                char_index,
                byte_offset,
                vowel,
            });
        }
    }
    target
}
