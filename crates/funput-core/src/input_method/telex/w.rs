use crate::composition::uo_horn::uo_pair_in_vowel_cluster;
use crate::input_method::KeyAction;
use crate::unicode::marks::{is_vowel, tone_on_vowel, vowel_stem};
use crate::unicode::shapes::{VowelShape, shape_on_vowel, shape_target_index};

use super::last_char;

fn plain(ch: char, stem: char) -> bool {
    vowel_stem(ch).is_some_and(|value| value.eq_ignore_ascii_case(&stem))
        && tone_on_vowel(ch).is_none()
        && shape_on_vowel(ch).is_none()
}

fn ends_with_ua_target(buffer: &str) -> bool {
    let mut rev = buffer.chars().rev();
    if !rev.next().is_some_and(|ch| plain(ch, 'a')) {
        return false;
    }
    let is_u = rev
        .next()
        .and_then(vowel_stem)
        .is_some_and(|stem| matches!(stem, 'u' | 'U' | 'ư' | 'Ư'));
    is_u && !matches!(rev.next(), Some('q' | 'Q'))
}

pub(super) fn classify(buffer: &str) -> Option<KeyAction> {
    if uo_pair_in_vowel_cluster(buffer).is_some() {
        return horn();
    }
    let last = last_char(buffer)?;
    if is_vowel(last) {
        if let Some(shape) = shape_on_vowel(last) {
            return match shape {
                VowelShape::Breve | VowelShape::Horn => Some(KeyAction::Shape(shape)),
                VowelShape::Circumflex => None,
            };
        }
        if plain(last, 'a') {
            return if ends_with_ua_target(buffer) {
                horn()
            } else {
                Some(KeyAction::Shape(VowelShape::Breve))
            };
        }
        if plain(last, 'o') || plain(last, 'u') {
            return horn();
        }
    }
    if shape_target_index(buffer, VowelShape::Breve).is_some() {
        return Some(KeyAction::Shape(VowelShape::Breve));
    }
    if shape_target_index(buffer, VowelShape::Horn).is_some() {
        return horn();
    }
    None
}

fn horn() -> Option<KeyAction> {
    Some(KeyAction::Shape(VowelShape::Horn))
}
