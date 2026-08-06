use super::*;

#[test]
fn numpad_digits_are_tagged_numpad() {
    // VK_NUMPAD0..=VK_NUMPAD9 (0x60..=0x69) → literal numbers in the engine.
    for vk in 0x60..=0x69u16 {
        assert_eq!(key_source(VIRTUAL_KEY(vk)), KeySource::Numpad);
    }
}

#[test]
fn top_row_digits_and_letters_stay_standard() {
    // Top-row '0'..'9' (0x30..=0x39) keep their VNI-modifier role, as do letters
    // and the numpad operators (`*` 0x6A .. `/` 0x6F) which are not digits.
    for vk in [0x30u16, 0x35, 0x39, 0x41, 0x5A, 0x6A, 0x6F] {
        assert_eq!(key_source(VIRTUAL_KEY(vk)), KeySource::Standard);
    }
}
