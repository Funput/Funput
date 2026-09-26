//! When two spellings are the same word.
//!
//! `khóa` and `khoá` are one word with the tone mark on different vowels — the old
//! and new placement rules, and the engine writes whichever the user's setting asks
//! for. A harness that compares strings counts every such repair as a wrong one,
//! which once made up more than half of the "wrong" column.

use unicode_normalization::UnicodeNormalization;

/// The five tone marks as NFD combining characters: huyền, sắc, ngã, hỏi, nặng.
const TONES: [char; 5] = ['\u{0300}', '\u{0301}', '\u{0303}', '\u{0309}', '\u{0323}'];

/// Whether `left` and `right` spell the same word, wherever the tone mark sits.
pub(super) fn same_word(left: &str, right: &str) -> bool {
    left == right || split(left) == split(right)
}

/// The letters with their tone marks lifted out, and the marks in order.
fn split(word: &str) -> (String, Vec<char>) {
    let mut letters = String::with_capacity(word.len());
    let mut tones = Vec::new();
    for c in word.nfd() {
        if TONES.contains(&c) {
            tones.push(c);
        } else {
            letters.push(c);
        }
    }
    (letters, tones)
}

#[cfg(test)]
mod tests {
    use super::same_word;

    #[test]
    fn a_moved_tone_mark_is_the_same_word() {
        assert!(same_word("khóa", "khoá"));
        assert!(same_word("thủy", "thuỷ"));
    }

    #[test]
    fn a_different_tone_or_letter_is_a_different_word() {
        assert!(!same_word("khóa", "khòa"));
        assert!(!same_word("trước", "rước"));
        assert!(!same_word("dải", "dài"));
    }
}
