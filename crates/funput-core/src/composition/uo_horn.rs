//! The `uo` → `ươ` horn-compound rules (apply, complete, revert).
//!
//! For an adjacent plain `uo`, the horn initially lands only on the `o` (`uơ`):
//! that is the completed open rhyme in `thuở`/`huơ`/`quơ`. If another vowel or
//! coda arrives, the compound completes to `ươ` (`thương`, `hươu`). All
//! helpers here are allocation-free except the ones that build the new buffer.

use crate::unicode::marks::{is_vowel, tone_on_vowel, vowel_stem};
use crate::unicode::shapes::{VowelShape, apply_shape, shape_on_vowel, strip_shape};

/// Char indices of the first adjacent plain `uo` pair inside the vowel cluster.
/// The ASCII match already excludes `ư`, `ơ`, and any toned variants.
pub(crate) fn uo_pair_in_vowel_cluster(buffer: &str) -> Option<(usize, usize)> {
    let mut in_cluster = false;
    let mut prev: Option<(usize, char)> = None; // previous cluster vowel
    for (i, ch) in buffer.chars().enumerate() {
        if is_vowel(ch) {
            if let Some((u_idx, u)) = prev
                && u.eq_ignore_ascii_case(&'u')
                && ch.eq_ignore_ascii_case(&'o')
            {
                return Some((u_idx, i));
            }
            in_cluster = true;
            prev = Some((i, ch));
        } else if in_cluster {
            break;
        }
    }
    None
}

/// Horn the `uo` pair: only the `o` for the open rhyme (`uơ`), both vowels once
/// something follows — unless the `u` is a `qu` onset glide, which never horns.
pub(crate) fn apply_uo_compound(buffer: &str) -> Option<String> {
    let (u_idx, o_idx) = uo_pair_in_vowel_cluster(buffer)?;
    let has_continuation = o_idx + 1 < buffer.chars().count();
    let is_qu_glide = u_idx > 0
        && buffer
            .chars()
            .nth(u_idx - 1)
            .is_some_and(|c| c.eq_ignore_ascii_case(&'q'));
    let horn_u = has_continuation && !is_qu_glide;

    let mut horned = String::with_capacity(buffer.len() + 2);
    for (i, ch) in buffer.chars().enumerate() {
        let ch = if i == o_idx || (i == u_idx && horn_u) {
            apply_shape(ch, VowelShape::Horn)?
        } else {
            ch
        };
        horned.push(ch);
    }
    Some(horned)
}

/// Return the byte offset and character of the plain `u` when `buffer` ends in
/// ambiguous `uơ` (including a tone on `ơ`), plus the character before `u`.
/// The ASCII-byte guard makes the overwhelmingly common path constant-time.
fn open_uo_horn_suffix(buffer: &str) -> Option<(usize, char, Option<char>)> {
    if buffer.as_bytes().last().is_none_or(u8::is_ascii) {
        return None;
    }

    let mut chars = buffer.char_indices().rev();
    let (_, o) = chars.next()?;
    let (u_offset, u) = chars.next()?;
    let before_u = chars.next().map(|(_, ch)| ch);
    let is_plain_u = vowel_stem(u).is_some_and(|stem| stem.eq_ignore_ascii_case(&'u'))
        && shape_on_vowel(u).is_none();
    let is_horned_o = vowel_stem(o).is_some_and(|stem| stem.eq_ignore_ascii_case(&'ơ'));
    (is_plain_u && is_horned_o).then_some((u_offset, u, before_u))
}

/// Complete an ambiguous open `uơ` as `ươ` once another character proves that
/// the rhyme continues (`thuơ` + `n` → `thươn`, `huơ` + `u` → `hươu`). The `u`
/// in a `qu` onset is a glide, not part of the nucleus, so `quơi` stays `quơi`.
pub(crate) fn complete_uo_horn_for_continuation(buffer: &str, key: char) -> Option<String> {
    let (u_offset, u, before_u) = open_uo_horn_suffix(buffer)?;
    if before_u.is_some_and(|ch| ch.eq_ignore_ascii_case(&'q')) {
        return None;
    }

    let shaped_u = apply_shape(u, VowelShape::Horn)?;
    let after_u = u_offset + u.len_utf8();
    let mut completed = String::with_capacity(buffer.len() + key.len_utf8() + 1);
    completed.push_str(&buffer[..u_offset]);
    completed.push(shaped_u);
    completed.push_str(&buffer[after_u..]);
    completed.push(key);
    Some(completed)
}

/// Whether the buffer ends in the ambiguous open `uơ` form. A repeated horn key
/// must revert this form (`uo77` → `uo7`) rather than horn the remaining `u`.
pub(crate) fn ends_with_open_uo_horn(buffer: &str) -> bool {
    open_uo_horn_suffix(buffer).is_some()
}

/// Revert a trailing plain (untoned) `ươ` back to `uo` when the horn key is
/// pressed again. `None` when the buffer does not end in that compound.
pub(crate) fn try_revert_uo_compound(buffer: &str) -> Option<String> {
    let mut chars = buffer.char_indices().rev();
    let (_, o) = chars.next()?;
    let (u_offset, u) = chars.next()?;

    let plain_horned = |ch: char, base: char| {
        shape_on_vowel(ch) == Some(VowelShape::Horn)
            && tone_on_vowel(ch).is_none()
            && strip_shape(ch).is_some_and(|c| c.eq_ignore_ascii_case(&base))
    };
    if !plain_horned(u, 'u') || !plain_horned(o, 'o') {
        return None;
    }

    let mut reverted = String::with_capacity(buffer.len());
    reverted.push_str(&buffer[..u_offset]);
    reverted.push(strip_shape(u)?);
    reverted.push(strip_shape(o)?);
    Some(reverted)
}
