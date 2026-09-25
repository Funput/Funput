//! Words typed on purpose that typo correction must leave exactly as typed.
//!
//! Chat abbreviations (`ko`, `dc`, `mk`), brand names, names typed without their
//! marks: none is a Vietnamese syllable, several sit one key from one, and a touch
//! near a key's edge makes them look exactly like a slip. `funput-engine` cannot tell
//! them apart, so the platform's veto has to know them by name.
//!
//! The list is `data/correction/keep.txt` — hand-written, MIT — compiled in and read
//! in place. A linear scan of about 130 words, with no parse step and no allocation,
//! is cheaper than anything that would have to be built first, and it is asked once
//! per finished word.

const KEEP: &str = include_str!("../data/correction/keep.txt");

/// Whether `word` is on the list, ignoring case.
pub(crate) fn is_kept(word: &str) -> bool {
    words().any(|kept| kept.chars().eq(word.chars().flat_map(char::to_lowercase)))
}

fn words() -> impl Iterator<Item = &'static str> {
    KEEP.lines()
        .map(|line| line.split('#').next().unwrap_or_default())
        .flat_map(str::split_whitespace)
}

#[cfg(test)]
mod tests {
    use super::{is_kept, words};

    #[test]
    fn chat_abbreviations_and_names_are_kept_in_any_case() {
        assert!(is_kept("ko"));
        assert!(is_kept("Ko"));
        assert!(is_kept("đc"));
        assert!(is_kept("Đc"));
        assert!(is_kept("shopee"));
        assert!(!is_kept("kox"));
        assert!(!is_kept("k o"));
        assert!(!is_kept(""));
    }

    /// Matching folds the typed word to lowercase and compares it with the list as
    /// written, so an entry with a capital could never match anything.
    #[test]
    fn the_list_is_written_in_lowercase() {
        for word in words() {
            assert!(
                word.chars().all(|c| !c.is_uppercase()),
                "{word} must be written in lowercase"
            );
        }
    }
}
