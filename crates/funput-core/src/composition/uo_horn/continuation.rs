use crate::unicode::marks::{apply_tone_to_vowel, is_vowel, tone_on_vowel};
use crate::unicode::shapes::{
    VowelShape, apply_shape_to_vowel, base_vowel, shape_on_vowel, strip_shape,
};

use super::pair::{horned_uo_suffix, open_uo_suffix};

/// Drop the stray `u` of a `ưu` that a vowel key continues past.
///
/// `ưu` is a closed rhyme: across Viet74K's 73 901 entries, 658 contain it and
/// **none** carry another letter after it. So a vowel arriving here means the `u`
/// was never part of the rhyme — it is the literal keystroke left over when a
/// horn key already produced the `ư` on its own. Dropping it hands the syllable
/// back to the ordinary horn compound: `trưu` + `o` → `trưo` → `trươ` → `trường`.
///
/// This is what lets a horn typed *before* the nucleus still reach `ươ`, whether
/// it came from Full Telex's leading `w` (`trwuongf`), the `[` shortcut
/// (`tr[uongf`) or VNI's `7`.
pub(crate) fn drop_stray_u_after_horn_u(buffer: &str, key: char) -> Option<String> {
    if !is_vowel(key) {
        return None;
    }
    let mut chars = buffer.char_indices().rev();
    let (u_offset, u) = chars.next()?;
    if !matches!(u, 'u' | 'U') {
        return None;
    }
    let (_, horn) = chars.next()?;
    if shape_on_vowel(horn) != Some(VowelShape::Horn)
        || !base_vowel(horn).is_some_and(|base| base.eq_ignore_ascii_case(&'u'))
    {
        return None;
    }
    let mut text = String::with_capacity(u_offset + key.len_utf8());
    text.push_str(&buffer[..u_offset]);
    text.push(key);
    Some(text)
}

#[inline]
pub(crate) fn complete_uo_horn_for_continuation(buffer: &str, key: char) -> Option<String> {
    let bytes = buffer.as_bytes();
    match bytes.last().copied()? {
        b'o' | b'O' => {
            if bytes.len() < 3 || bytes[bytes.len() - 2].is_ascii() {
                return None;
            }
            return complete_horned_uo_for_continuation(buffer, key);
        }
        byte if byte.is_ascii() => return None,
        _ => {}
    }
    let (u_offset, u, before) = open_uo_suffix(buffer)?;
    if before.is_some_and(|ch| ch.eq_ignore_ascii_case(&'q')) {
        return None;
    }
    let shaped = apply_shape_to_vowel(u, VowelShape::Horn)?;
    Some(replace_and_append(buffer, u_offset, u, shaped, key))
}

pub(crate) fn complete_horned_uo_for_continuation(buffer: &str, key: char) -> Option<String> {
    let (_, _, o_offset, o) = horned_uo_suffix(buffer)?;
    let shaped = apply_shape_to_vowel(o, VowelShape::Horn)?;
    let shaped = tone_on_vowel(o)
        .and_then(|tone| apply_tone_to_vowel(shaped, tone))
        .unwrap_or(shaped);
    Some(replace_and_append(buffer, o_offset, o, shaped, key))
}

/// Open `ưo` resolves as `uơ` before a tone is applied (`thưo` + `r` → `thuở`).
pub(crate) fn normalize_horned_uo_open(buffer: &str) -> Option<String> {
    let (u_offset, u, o_offset, o) = horned_uo_suffix(buffer)?;
    let plain_u = strip_shape(u)?;
    let horned_o = apply_shape_to_vowel(o, VowelShape::Horn)?;
    let horned_o = tone_on_vowel(o)
        .and_then(|tone| apply_tone_to_vowel(horned_o, tone))
        .unwrap_or(horned_o);
    let mut output = String::with_capacity(buffer.len() + 1);
    output.push_str(&buffer[..u_offset]);
    output.push(plain_u);
    output.push_str(&buffer[u_offset + u.len_utf8()..o_offset]);
    output.push(horned_o);
    output.push_str(&buffer[o_offset + o.len_utf8()..]);
    Some(output)
}

fn replace_and_append(buffer: &str, offset: usize, old: char, new: char, key: char) -> String {
    let mut output = String::with_capacity(buffer.len() + key.len_utf8() + 1);
    output.push_str(&buffer[..offset]);
    output.push(new);
    output.push_str(&buffer[offset + old.len_utf8()..]);
    output.push(key);
    output
}
