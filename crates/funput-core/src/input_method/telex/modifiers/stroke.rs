use crate::input_method::KeyAction;
use crate::unicode::marks::is_vowel;

use super::super::last_char;

fn target_before_vowel(buffer: &str) -> bool {
    let mut seen_d = false;
    for ch in buffer.chars() {
        if matches!(ch, 'd' | 'D' | 'đ' | 'Đ') {
            seen_d = true;
        } else if seen_d && is_vowel(ch) {
            return true;
        }
    }
    false
}

pub(crate) fn classify(buffer: &str, key: char) -> Option<KeyAction> {
    if !key.eq_ignore_ascii_case(&'d') {
        return None;
    }
    if last_char(buffer)
        .is_some_and(|last| last.eq_ignore_ascii_case(&'d') || matches!(last, 'đ' | 'Đ'))
        || target_before_vowel(buffer)
    {
        return Some(KeyAction::Stroke);
    }
    None
}
