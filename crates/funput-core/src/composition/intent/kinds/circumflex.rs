use crate::ToneStyle;
use crate::composition::replace_char_at;
use crate::input_method::telex::tone_from_key;
use crate::orthography::reposition_existing_tone;
use crate::unicode::marks::Tone;
use crate::unicode::shapes::{VowelShape, shape_on_vowel, strip_shape};
use crate::validation::syllable::is_viable_shape_candidate;

use super::super::IntentResolution;
use super::super::candidate;
use super::super::target::{Target, rightmost_stem};

pub(crate) fn resolve(buffer: &str, stem: char, key: char, style: ToneStyle) -> IntentResolution {
    let Some(target) = rightmost_stem(buffer, stem) else {
        return literal(buffer, key, None);
    };
    if shape_on_vowel(target.vowel) == Some(VowelShape::Circumflex) {
        return revert(buffer, key, target);
    }

    let mut text = String::with_capacity(buffer.len() + 2);
    candidate::build(&mut text, buffer, target, None);
    // The new mũ makes the shaped vowel the tonal nucleus, so a tone already
    // placed on another vowel has to follow it: `duỵ` + `et` + `e` → `duyệt`,
    // not `duỵêt`. Without the move the candidate is also mis-spelled and would
    // fail the viability check below, dropping the key as a literal.
    if let Some(moved) = reposition_existing_tone(&text, style) {
        text = moved;
    }
    if is_viable_shape_candidate(&text) {
        return IntentResolution::Applied(text);
    }
    if let Some((offset, tone)) = pending_tone(buffer, target.byte_offset) {
        text.clear();
        candidate::build(&mut text, buffer, target, Some(offset));
        if candidate::apply_tone(&mut text, tone, style) && is_viable_shape_candidate(&text) {
            return IntentResolution::Applied(text);
        }
    }
    literal(buffer, key, Some(text))
}

fn revert(buffer: &str, key: char, target: Target) -> IntentResolution {
    let Some(plain) = strip_shape(target.vowel) else {
        return literal(buffer, key, None);
    };
    let mut text = replace_char_at(buffer, target.char_index, plain);
    text.push(key);
    IntentResolution::Reverted(text)
}

fn pending_tone(buffer: &str, before: usize) -> Option<(usize, Tone)> {
    buffer[..before]
        .char_indices()
        .rev()
        .find_map(|(offset, ch)| tone_from_key(ch).map(|tone| (offset, tone)))
}

fn literal(buffer: &str, key: char, reusable: Option<String>) -> IntentResolution {
    let mut text = reusable.unwrap_or_else(|| String::with_capacity(buffer.len() + key.len_utf8()));
    text.clear();
    text.push_str(buffer);
    text.push(key);
    IntentResolution::Literal(text)
}
