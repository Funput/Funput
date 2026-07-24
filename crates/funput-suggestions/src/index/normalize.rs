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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn normalizes_case_nfc_and_vietnamese_marks() {
        assert_eq!(exact("A\u{0301}NH"), "ánh");
        assert_eq!(folded("Đường"), "duong");
        assert_eq!(folded("HOÀ"), "hoa");
    }
}
