use crate::unicode::marks::tone_on_vowel;
use crate::unicode::shapes::{VowelShape, shape_on_vowel, strip_shape};

pub(crate) fn try_revert_uo_compound(buffer: &str) -> Option<String> {
    let mut chars = buffer.char_indices().rev();
    let (_, o) = chars.next()?;
    let (u_offset, u) = chars.next()?;
    let plain_horned = |ch: char, base: char| {
        shape_on_vowel(ch) == Some(VowelShape::Horn)
            && tone_on_vowel(ch).is_none()
            && strip_shape(ch).is_some_and(|value| value.eq_ignore_ascii_case(&base))
    };
    if !plain_horned(u, 'u') || !plain_horned(o, 'o') {
        return None;
    }
    let mut output = String::with_capacity(buffer.len());
    output.push_str(&buffer[..u_offset]);
    output.push(strip_shape(u)?);
    output.push(strip_shape(o)?);
    Some(output)
}
