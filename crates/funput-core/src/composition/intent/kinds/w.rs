use crate::composition::uo_horn::{apply_uo_compound_in_place, uo_pair_in_vowel_cluster};
use crate::input_method::KeyAction;
use crate::input_method::telex::classify_w;
use crate::orthography::glide;
use crate::unicode::marks::{apply_tone_to_vowel, is_vowel, tone_on_vowel};
use crate::unicode::shapes::{apply_shape_to_vowel, shape_target_index};
use crate::validation::syllable::is_viable_shape_candidate;

use super::super::IntentResolution;

/// Exactly one non-leading `w` before the first vowel is a deferred modifier.
#[inline(always)]
pub(crate) fn has_pending(buffer: &str) -> bool {
    let bytes = buffer.as_bytes();
    if bytes.len() < 2 || (!bytes.contains(&b'w') && !bytes.contains(&b'W')) {
        return false;
    }
    let mut count = 0;
    for (offset, ch) in buffer.char_indices() {
        // An onset glide is not the nucleus, so the marker is still "before the
        // vowel" past it: `quw` + `own` → `quơn`, `giw` + `of` → `giờ`.
        if is_vowel(ch) && glide::at(buffer, offset).is_none() {
            break;
        }
        if offset > 0 && ch.eq_ignore_ascii_case(&'w') {
            count += 1;
        }
    }
    count == 1
}

pub(crate) fn resolve(buffer: &str, key: char) -> IntentResolution {
    let Some(w_offset) = pending_offset(buffer) else {
        return IntentResolution::Literal(appended(buffer, key));
    };
    if matches!(key, 'd' | 'D') && has_only_stroke_target(buffer, w_offset) {
        return super::stroke::resolve(buffer, key);
    }
    let mut candidate = String::with_capacity(buffer.len() + key.len_utf8() + 2);
    candidate.push_str(&buffer[..w_offset]);
    candidate.push_str(&buffer[w_offset + 1..]);
    candidate.push(key);

    let Some(KeyAction::Shape(shape)) = classify_w(&candidate) else {
        return raw(buffer, key, w_offset, candidate);
    };
    if apply_in_place(&mut candidate, shape) && is_viable_shape_candidate(&candidate) {
        return IntentResolution::Applied(candidate);
    }
    raw(buffer, key, w_offset, candidate)
}

fn has_only_stroke_target(buffer: &str, w_offset: usize) -> bool {
    let mut chars = buffer[..w_offset]
        .chars()
        .chain(buffer[w_offset + 1..].chars());
    chars
        .next()
        .is_some_and(|ch| matches!(ch, 'd' | 'D' | 'đ' | 'Đ'))
        && chars.next().is_none()
}

fn apply_in_place(text: &mut String, shape: crate::unicode::shapes::VowelShape) -> bool {
    if uo_pair_in_vowel_cluster(text).is_some() {
        return apply_uo_compound_in_place(text);
    }
    let Some(index) = shape_target_index(text, shape) else {
        return false;
    };
    let Some((offset, vowel)) = text.char_indices().nth(index) else {
        return false;
    };
    let Some(bare) = apply_shape_to_vowel(vowel, shape) else {
        return false;
    };
    let shaped = tone_on_vowel(vowel)
        .and_then(|tone| apply_tone_to_vowel(bare, tone))
        .unwrap_or(bare);
    text.replace_range(
        offset..offset + vowel.len_utf8(),
        shaped.encode_utf8(&mut [0; 4]),
    );
    true
}

fn pending_offset(buffer: &str) -> Option<usize> {
    if !has_pending(buffer) {
        return None;
    }
    buffer
        .char_indices()
        .find(|&(offset, ch)| offset > 0 && ch.eq_ignore_ascii_case(&'w'))
        .map(|(offset, _)| offset)
}

fn raw(buffer: &str, key: char, w_offset: usize, mut reusable: String) -> IntentResolution {
    reusable.clear();
    if has_only_stroke_target(buffer, w_offset) && buffer.contains(['đ', 'Đ']) {
        for ch in buffer.chars() {
            reusable.push(match ch {
                'đ' => 'd',
                'Đ' => 'D',
                _ => ch,
            });
        }
        reusable.push(if buffer.contains('Đ') { 'D' } else { 'd' });
        reusable.push(key);
        return IntentResolution::Deferred(reusable);
    }
    reusable.push_str(buffer);
    reusable.push(key);
    IntentResolution::Deferred(reusable)
}

fn appended(buffer: &str, key: char) -> String {
    let mut text = String::with_capacity(buffer.len() + key.len_utf8());
    text.push_str(buffer);
    text.push(key);
    text
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn marker_is_non_leading_unique_and_before_nucleus() {
        assert!(has_pending("lw"));
        assert!(has_pending("trw"));
        assert!(!has_pending("w"));
        assert!(!has_pending("lww"));
        assert!(!has_pending("law"));
    }

    #[test]
    fn pending_marker_allows_one_stroke_intent() {
        assert_eq!(resolve("dw", 'd'), IntentResolution::Applied("đw".into()));
        assert_eq!(resolve("dW", 'D'), IntentResolution::Applied("đW".into()));
        assert_eq!(resolve("đw", 'd'), IntentResolution::Reverted("dwd".into()));
        assert_eq!(
            resolve("gdw", 'd'),
            IntentResolution::Deferred("gdwd".into())
        );
        assert_eq!(
            resolve("đw", 'e'),
            IntentResolution::Deferred("dwde".into())
        );
    }
}
