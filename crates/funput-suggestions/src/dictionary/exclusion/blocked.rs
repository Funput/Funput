//! The words the user has blocked: a sorted list, searched without allocating.

use crate::index::normalize;

/// Exact-normalized, sorted and free of duplicates. Sorted by `str` order, which is
/// code point order, so a search can compare a word's normalized characters one by
/// one instead of building the key first.
#[derive(Debug, Default)]
pub(crate) struct BlockedWords {
    words: Vec<String>,
}

impl BlockedWords {
    /// Takes a list read off disk. Sorted again rather than trusted: the checksum
    /// rules out damage, not a writer from a build that ordered it differently.
    pub(crate) fn from_loaded(mut words: Vec<String>) -> Self {
        words.sort_unstable();
        words.dedup();
        Self { words }
    }

    /// Whether `word` is blocked, in any case or normalization form.
    pub(crate) fn contains(&self, word: &str) -> bool {
        self.position(word).is_ok()
    }

    /// Adds an exact-normalized word. False when it was already there.
    pub(crate) fn insert(&mut self, exact: String) -> bool {
        match self.position(&exact) {
            Ok(_) => false,
            Err(at) => {
                self.words.insert(at, exact);
                true
            }
        }
    }

    pub(crate) fn remove(&mut self, word: &str) -> bool {
        match self.position(word) {
            Ok(at) => {
                self.words.remove(at);
                true
            }
            Err(_) => false,
        }
    }

    pub(crate) fn len(&self) -> usize {
        self.words.len()
    }

    pub(crate) fn as_slice(&self) -> &[String] {
        &self.words
    }

    pub(crate) fn heap_bytes(&self) -> usize {
        self.words.capacity() * size_of::<String>()
            + self.words.iter().map(String::capacity).sum::<usize>()
    }

    fn position(&self, word: &str) -> Result<usize, usize> {
        self.words
            .binary_search_by(|blocked| blocked.chars().cmp(normalize::exact_chars(word)))
    }
}

#[cfg(test)]
mod tests {
    use super::BlockedWords;

    #[test]
    fn matches_in_any_case_and_keeps_its_order() {
        let mut blocked = BlockedWords::from_loaded(vec!["zalo".into(), "đm".into()]);
        assert!(blocked.insert("deadline".into()));
        assert!(!blocked.insert("deadline".into()));
        assert!(blocked.contains("Zalo"));
        assert!(blocked.contains("ĐM"));
        assert!(!blocked.contains("dm"));
        assert_eq!(blocked.as_slice(), ["deadline", "zalo", "đm"]);
        assert!(blocked.remove("ZALO"));
        assert!(!blocked.remove("zalo"));
        assert_eq!(blocked.len(), 2);
    }

    #[test]
    fn a_loaded_list_is_sorted_and_deduplicated() {
        let blocked = BlockedWords::from_loaded(vec!["b".into(), "a".into(), "b".into()]);
        assert_eq!(blocked.as_slice(), ["a", "b"]);
    }
}
