use crate::composition::apply::apply_shape_key;
use crate::composition::uo_horn::uo_pair_in_vowel_cluster;
use crate::input_method::KeyAction;
use crate::input_method::telex::classify_w;
use crate::unicode::marks::{apply_tone_to_vowel, is_vowel, tone_on_vowel};
use crate::unicode::shapes::{apply_shape_to_vowel, shape_target_index};
use crate::validation::syllable::is_viable_shape_candidate;

use super::IntentResolution;

/// Exactly one non-leading `w` before the first vowel is a deferred modifier.
pub(crate) fn has_pending(buffer: &str) -> bool {
    let mut count = 0;
    for (index, ch) in buffer.chars().enumerate() {
        if is_vowel(ch) {
            break;
        }
        if index > 0 && ch.eq_ignore_ascii_case(&'w') {
            count += 1;
        }
    }
    count == 1
}

pub(super) fn resolve(buffer: &str, key: char) -> IntentResolution {
    let Some(w_offset) = pending_offset(buffer) else {
        return IntentResolution::Literal(appended(buffer, key));
    };
    let mut candidate = String::with_capacity(buffer.len() + key.len_utf8() + 2);
    candidate.push_str(&buffer[..w_offset]);
    candidate.push_str(&buffer[w_offset + 1..]);
    candidate.push(key);

    let Some(KeyAction::Shape(shape)) = classify_w(&candidate) else {
        return raw(buffer, key, candidate);
    };
    if apply_in_place(&mut candidate, shape) && is_viable_shape_candidate(&candidate) {
        return IntentResolution::Applied(candidate);
    }
    raw(buffer, key, candidate)
}

fn apply_in_place(text: &mut String, shape: crate::unicode::shapes::VowelShape) -> bool {
    if uo_pair_in_vowel_cluster(text).is_some() {
        *text = apply_shape_key(text, shape).text;
        return true;
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

fn raw(buffer: &str, key: char, mut reusable: String) -> IntentResolution {
    reusable.clear();
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
}
