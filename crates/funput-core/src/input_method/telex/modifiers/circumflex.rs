use crate::input_method::{CircumflexStem, KeyAction};
use crate::unicode::marks::{tone_on_vowel, vowel_stem};
use crate::unicode::shapes::{VowelShape, base_vowel, shape_on_vowel};

use super::super::last_char;

fn is_plain_stem(ch: char, stem: char) -> bool {
    vowel_stem(ch).is_some_and(|value| value.eq_ignore_ascii_case(&stem))
        && tone_on_vowel(ch).is_none()
        && shape_on_vowel(ch).is_none()
}

pub(crate) fn classify(buffer: &str, key: char) -> Option<KeyAction> {
    let stem = CircumflexStem::from_key(key)?;
    let stem_char = stem.as_char();
    if let Some(last) = last_char(buffer) {
        if is_plain_stem(last, stem_char) {
            return Some(KeyAction::Shape(VowelShape::Circumflex));
        }
        // Any shaped vowel on the same base letter is a target: an existing mũ
        // means the key reverts it (`aaa` → `aa`), a trần/móc means it switches
        // to mũ (`ăa` → `â`). `apply_shape_to_vowel` tells the two apart.
        if shape_on_vowel(last).is_some()
            && base_vowel(last).is_some_and(|base| base.eq_ignore_ascii_case(&stem_char))
        {
            return Some(KeyAction::Shape(VowelShape::Circumflex));
        }
    }
    free_position(buffer, stem)
}

#[inline]
fn free_position(buffer: &str, stem: CircumflexStem) -> Option<KeyAction> {
    let stem_char = stem.as_char();
    let mut ascii = true;
    for byte in buffer.bytes().rev() {
        if !byte.is_ascii() {
            ascii = false;
            break;
        }
        if !byte.is_ascii_alphabetic() {
            return None;
        }
        if byte.eq_ignore_ascii_case(&(stem_char as u8)) {
            return Some(KeyAction::FreeCircumflex(stem));
        }
    }
    if ascii {
        return None;
    }
    buffer
        .chars()
        .rev()
        .take_while(|ch| ch.is_alphabetic())
        .find(|&vowel| {
            base_vowel(vowel).is_some_and(|target| target.eq_ignore_ascii_case(&stem_char))
        })
        .map(|_| KeyAction::FreeCircumflex(stem))
}
