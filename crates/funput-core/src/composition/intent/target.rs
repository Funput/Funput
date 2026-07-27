use crate::unicode::shapes::base_vowel;

#[derive(Debug, Clone, Copy)]
pub(super) struct Target {
    pub char_index: usize,
    pub byte_offset: usize,
    pub vowel: char,
}

pub(super) fn rightmost_stem(buffer: &str, stem: char) -> Option<Target> {
    let mut target = None;
    for (char_index, (byte_offset, vowel)) in buffer.char_indices().enumerate() {
        if !vowel.is_alphabetic() {
            target = None;
            continue;
        }
        // Compared on the base letter, so a vowel that already carries a trần/móc
        // is still a mũ target and switches shape (`chặn` + `a` → `chận`).
        if base_vowel(vowel).is_some_and(|value| value.eq_ignore_ascii_case(&stem)) {
            target = Some(Target {
                char_index,
                byte_offset,
                vowel,
            });
        }
    }
    target
}
