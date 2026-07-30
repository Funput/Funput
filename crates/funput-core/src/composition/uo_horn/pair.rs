use crate::unicode::marks::{is_vowel, vowel_stem};
use crate::unicode::shapes::{VowelShape, shape_on_vowel};

fn plain_stem(ch: char, base: char) -> bool {
    if ch.eq_ignore_ascii_case(&base) {
        return true;
    }
    shape_on_vowel(ch).is_none()
        && vowel_stem(ch).is_some_and(|stem| stem.eq_ignore_ascii_case(&base))
}

/// Adjacent unshaped `uo`, allowing either vowel to already carry a tone.
pub(crate) fn uo_pair_in_vowel_cluster(buffer: &str) -> Option<(usize, usize)> {
    let mut in_cluster = false;
    let mut previous = None;
    for (index, ch) in buffer.chars().enumerate() {
        if is_vowel(ch) {
            if let Some((u_index, u)) = previous
                && plain_stem(u, 'u')
                && plain_stem(ch, 'o')
            {
                return Some((u_index, index));
            }
            in_cluster = true;
            previous = Some((index, ch));
        } else if in_cluster {
            break;
        }
    }
    None
}

/// Trailing plain `u` + horned `o` (`thưo`-style, before the pair resolves).
pub(super) fn open_uo_suffix(buffer: &str) -> Option<(usize, char)> {
    if buffer.as_bytes().last().is_none_or(u8::is_ascii) {
        return None;
    }
    let mut chars = buffer.char_indices().rev();
    let (_, o) = chars.next()?;
    let (u_offset, u) = chars.next()?;
    let plain_u = plain_stem(u, 'u');
    let horned_o = shape_on_vowel(o) == Some(VowelShape::Horn);
    (plain_u && horned_o).then_some((u_offset, u))
}

pub(super) fn horned_uo_suffix(buffer: &str) -> Option<(usize, char, usize, char)> {
    let bytes = buffer.as_bytes();
    if bytes.len() < 3
        || !matches!(bytes.last(), Some(b'o' | b'O'))
        || bytes[bytes.len() - 2].is_ascii()
    {
        return None;
    }
    let mut chars = buffer.char_indices().rev();
    let (o_offset, o) = chars.next()?;
    let (u_offset, u) = chars.next()?;
    let horned_u = shape_on_vowel(u) == Some(VowelShape::Horn);
    (horned_u && plain_stem(o, 'o')).then_some((u_offset, u, o_offset, o))
}

pub(crate) fn ends_with_open_uo_horn(buffer: &str) -> bool {
    open_uo_suffix(buffer).is_some()
}
