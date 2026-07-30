use crate::ToneStyle;
use crate::orthography::{tone_target_vowel, tone_vowel_index};
use crate::unicode::marks::{Tone, apply_tone_to_vowel, tone_on_vowel};
use crate::unicode::shapes::{VowelShape, apply_shape_to_vowel};

use super::target::Target;

pub(super) fn build(
    output: &mut String,
    buffer: &str,
    target: Target,
    skipped_byte: Option<usize>,
) {
    for (offset, ch) in buffer.char_indices() {
        if skipped_byte == Some(offset) {
            continue;
        }
        if offset != target.byte_offset {
            output.push(ch);
            continue;
        }
        let Some(shaped) = apply_shape_to_vowel(ch, VowelShape::Circumflex) else {
            output.push(ch);
            continue;
        };
        output.push(match tone_on_vowel(ch) {
            Some(tone) => apply_tone_to_vowel(shaped, tone).unwrap_or(shaped),
            None => shaped,
        });
    }
}

pub(super) fn apply_tone(text: &mut String, tone: Tone, style: ToneStyle) -> bool {
    let Some(index) = tone_vowel_index(text, style) else {
        return false;
    };
    let Some(vowel) = text.chars().nth(index) else {
        return false;
    };
    let target = tone_target_vowel(text, index).unwrap_or(vowel);
    let Some(toned) = apply_tone_to_vowel(target, tone) else {
        return false;
    };
    let Some((offset, current)) = text.char_indices().nth(index) else {
        return false;
    };
    text.replace_range(
        offset..offset + current.len_utf8(),
        toned.encode_utf8(&mut [0; 4]),
    );
    true
}
