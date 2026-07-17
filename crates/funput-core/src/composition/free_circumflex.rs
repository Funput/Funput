//! Free-position Telex circumflex (`aa` / `ee` / `oo`).
//!
//! The adjacent digraph stays on the established fast path. This module handles
//! only a repeated vowel that arrives after a tone or coda (`chana` → `chân`).
//! It targets an exact vowel stem, validates the resulting Vietnamese candidate,
//! and reuses one allocation for both the candidate and literal fallback.

use crate::composition::replace_char_at;
use crate::input_method::telex::tone_from_key;
use crate::unicode::marks::{Tone, apply_tone_to_vowel, tone_on_vowel, vowel_stem};
use crate::unicode::shapes::{VowelShape, apply_shape_to_vowel, shape_on_vowel, strip_shape};
use crate::unicode::tone_position::{tone_target_vowel, tone_vowel_index};
use crate::validation::syllable::is_viable_shape_candidate;
use crate::{ToneStyle, TransformKind, TransformResult};

#[derive(Debug, Clone, Copy)]
struct Target {
    char_index: usize,
    byte_offset: usize,
    vowel: char,
}

/// Apply or revert a free-position circumflex for the exact ASCII `stem`.
pub(crate) fn apply_free_circumflex(
    buffer: &str,
    key: char,
    stem: char,
    style: ToneStyle,
) -> TransformResult {
    let Some(target) = matching_target(buffer, stem) else {
        return literal(buffer, key, None);
    };

    if shape_on_vowel(target.vowel) == Some(VowelShape::Circumflex) {
        let Some(plain) = strip_shape(target.vowel) else {
            return literal(buffer, key, None);
        };
        let mut text = replace_char_at(buffer, target.char_index, plain);
        text.push(key);
        return TransformResult {
            kind: TransformKind::Reverted,
            text,
        };
    }

    let mut candidate = String::with_capacity(buffer.len() + 2);
    build_candidate(&mut candidate, buffer, target, None);
    if is_viable_shape_candidate(&candidate) {
        return applied(candidate);
    }

    // A tone typed before the first vowel is literal on the normal Telex path.
    // When the later repeated vowel makes the intended syllable unambiguous,
    // reinterpret that one pending tone without changing tone classification in
    // general (`chfana` → `chần`).
    if let Some((tone_offset, tone)) = pending_tone_before_target(buffer, target.byte_offset) {
        candidate.clear();
        build_candidate(&mut candidate, buffer, target, Some(tone_offset));
        if apply_tone_in_place(&mut candidate, tone, style) && is_viable_shape_candidate(&candidate)
        {
            return applied(candidate);
        }
    }

    literal(buffer, key, Some(candidate))
}

fn matching_target(buffer: &str, stem: char) -> Option<Target> {
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
        if vowel_stem(plain).is_some_and(|target| target.eq_ignore_ascii_case(&stem)) {
            target = Some(Target {
                char_index,
                byte_offset,
                vowel,
            });
        }
    }
    target
}

fn build_candidate(
    candidate: &mut String,
    buffer: &str,
    target: Target,
    skipped_byte: Option<usize>,
) {
    for (byte_offset, ch) in buffer.char_indices() {
        if skipped_byte == Some(byte_offset) {
            continue;
        }
        if byte_offset != target.byte_offset {
            candidate.push(ch);
            continue;
        }

        let Some(shaped) = apply_shape_to_vowel(ch, VowelShape::Circumflex) else {
            candidate.push(ch);
            continue;
        };
        candidate.push(match tone_on_vowel(ch) {
            Some(tone) => apply_tone_to_vowel(shaped, tone).unwrap_or(shaped),
            None => shaped,
        });
    }
}

fn pending_tone_before_target(buffer: &str, target_offset: usize) -> Option<(usize, Tone)> {
    buffer[..target_offset]
        .char_indices()
        .rev()
        .find_map(|(offset, ch)| tone_from_key(ch).map(|tone| (offset, tone)))
}

fn apply_tone_in_place(text: &mut String, tone: Tone, style: ToneStyle) -> bool {
    let Some(vowel_index) = tone_vowel_index(text, style) else {
        return false;
    };
    let Some(vowel) = text.chars().nth(vowel_index) else {
        return false;
    };
    let target = tone_target_vowel(text, vowel_index).unwrap_or(vowel);
    let Some(toned) = apply_tone_to_vowel(target, tone) else {
        return false;
    };
    let Some((offset, current)) = text.char_indices().nth(vowel_index) else {
        return false;
    };
    text.replace_range(
        offset..offset + current.len_utf8(),
        toned.encode_utf8(&mut [0; 4]),
    );
    true
}

fn applied(text: String) -> TransformResult {
    TransformResult {
        kind: TransformKind::Applied,
        text,
    }
}

fn literal(buffer: &str, key: char, reusable: Option<String>) -> TransformResult {
    let mut text = reusable.unwrap_or_else(|| String::with_capacity(buffer.len() + key.len_utf8()));
    text.clear();
    text.push_str(buffer);
    text.push(key);
    TransformResult {
        kind: TransformKind::Pending,
        text,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn apply(buffer: &str, key: char) -> TransformResult {
        apply_free_circumflex(
            buffer,
            key,
            key.to_ascii_lowercase(),
            ToneStyle::Traditional,
        )
    }

    #[test]
    fn applies_to_exact_stem_and_preserves_tone_and_case() {
        assert_eq!(apply("chan", 'a').text, "chân");
        assert_eq!(apply("chàn", 'a').text, "chần");
        assert_eq!(apply("ChAn", 'A').text, "ChÂn");
        assert_eq!(apply("hom", 'o').text, "hôm");
        assert_eq!(apply("dem", 'e').text, "dêm");
    }

    #[test]
    fn rejects_invalid_candidate_and_reuses_literal_key() {
        let result = apply("camer", 'a');
        assert_eq!(result.kind, TransformKind::Pending);
        assert_eq!(result.text, "camera");
        assert_eq!(apply("ngoe", 'o').text, "ngoeo");
        assert_eq!(apply("oao", 'o').text, "oaoo");
        assert_eq!(apply("â ", 'a').text, "â a");
    }

    #[test]
    fn reinterprets_early_tone_only_when_candidate_is_viable() {
        assert_eq!(apply("chfan", 'a').text, "chần");
    }

    #[test]
    fn reverts_non_adjacent_circumflex() {
        let result = apply("chân", 'a');
        assert_eq!(result.kind, TransformKind::Reverted);
        assert_eq!(result.text, "chana");
    }

    #[test]
    fn selects_the_rightmost_matching_stem() {
        let target = matching_target("oao", 'o').expect("matching o target");
        assert_eq!(target.char_index, 2);
        assert_eq!(target.byte_offset, 2);
    }
}
