use unicode_normalization::{UnicodeNormalization, char::is_combining_mark};

pub(crate) fn exact_chars(input: &str) -> impl Iterator<Item = char> + '_ {
    input.nfc().flat_map(char::to_lowercase)
}

pub(crate) fn folded_chars(input: &str) -> impl Iterator<Item = char> + '_ {
    input
        .nfd()
        .flat_map(char::to_lowercase)
        .filter(|ch| !is_combining_mark(*ch))
        .map(|ch| match ch {
            'đ' => 'd',
            'Đ' => 'D',
            _ => ch,
        })
}

pub(crate) fn exact(input: &str) -> String {
    exact_chars(input).collect()
}

pub(crate) fn folded(input: &str) -> String {
    folded_chars(input).collect()
}

/// Whether an [`exact`]-normalized word carries a mark folding would remove —
/// the same test that decides whether a word enters the folded trie. For the
/// words the engine learns, that is a Vietnamese tone or vowel mark, or `đ`.
/// Allocation-free, because the query path asks it.
pub(crate) fn is_marked(exact: &str) -> bool {
    folded_chars(exact).ne(exact.chars())
}

/// [`exact`] written onto `buffer` instead of the heap, for a query path that must
/// not allocate. `None` when the word does not fit, which for a buffer sized to
/// `MAX_TOKEN_SCALARS` means a word no list could hold.
pub(crate) fn exact_into<'b>(input: &str, buffer: &'b mut [u8]) -> Option<&'b str> {
    let mut len = 0;
    for scalar in exact_chars(input) {
        if len + scalar.len_utf8() > buffer.len() {
            return None;
        }
        len += scalar.encode_utf8(&mut buffer[len..]).len();
    }
    std::str::from_utf8(&buffer[..len]).ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn normalizes_case_nfc_and_vietnamese_marks() {
        assert_eq!(exact("A\u{0301}NH"), "ánh");
        assert_eq!(folded("Đường"), "duong");
        assert_eq!(folded("HOÀ"), "hoa");
    }

    #[test]
    fn a_word_is_marked_when_folding_changes_it() {
        for word in ["ăn", "thì", "đi", "khỏe"] {
            assert!(is_marked(&exact(word)), "{word}");
        }
        for word in ["anh", "hai", "work", "iphone"] {
            assert!(!is_marked(&exact(word)), "{word}");
        }
    }
}
