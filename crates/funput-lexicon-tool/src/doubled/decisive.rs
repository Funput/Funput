//! The decisive prefixes: where the engine may stop waiting for the word boundary.
//!
//! A listed word gets its raw keys back when it ends. Mid-word the engine can do it
//! sooner, the moment the shortened form on screen can no longer begin any English
//! word: nothing begins with `pasw`, so `password` shows `passw` at its `w`.
//! Design: `docs/features/doubled-key-restore.md` §5.3.
//!
//! A raw-key prefix is decisive when, in every setup where a revert has happened by
//! then, the word on screen (lowercased) begins no `en.tsv` word — one holding a
//! Vietnamese letter begins none — and the prefix itself begins no word the list
//! leaves out on purpose. That last rule keeps the switch from overriding the
//! frequency call: `off` is left out because `of` is the commoner word, so no
//! prefix that `off` starts with may switch to the raw keys either. Each listed word
//! contributes its shortest decisive prefix, if it has one.

use std::collections::BTreeSet;
use std::ops::Bound;

use super::scan::{self, SETUPS, Step};

/// A set of lowercase words, asked whether any of them starts with a prefix.
pub(crate) struct Words(BTreeSet<String>);

impl Words {
    pub(crate) fn new(words: impl IntoIterator<Item = String>) -> Self {
        Self(words.into_iter().collect())
    }

    pub(crate) fn any_starts_with(&self, prefix: &str) -> bool {
        self.0
            .range::<str, _>((Bound::Included(prefix), Bound::Unbounded))
            .next()
            .is_some_and(|word| word.starts_with(prefix))
    }
}

/// The shortest decisive prefix of each listed word that has one.
pub(crate) fn prefixes(
    listed: &BTreeSet<String>,
    english: &Words,
    excluded: &Words,
) -> BTreeSet<String> {
    listed
        .iter()
        .filter_map(|word| shortest(word, english, excluded))
        .collect()
}

fn shortest(word: &str, english: &Words, excluded: &Words) -> Option<String> {
    let setups: Vec<Vec<Step>> = SETUPS
        .iter()
        .map(|&(method, rules)| scan::typed(word, method, rules))
        .collect();
    (1..=word.len()).find_map(|len| {
        let prefix = &word[..len];
        let steps = setups.iter().map(|steps| &steps[len - 1]);
        let reverted = steps.clone().any(|step| step.reverted);
        let unambiguous = steps
            .filter(|step| step.reverted)
            .all(|step| !english.any_starts_with(&step.buffer.to_lowercase()));
        (reverted && unambiguous && !excluded.any_starts_with(prefix)).then(|| prefix.to_owned())
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    fn words(list: &[&str]) -> Words {
        Words::new(list.iter().map(|word| (*word).to_owned()))
    }

    fn decisive(word: &str, english: &[&str], excluded: &[&str]) -> Option<String> {
        shortest(word, &words(english), &words(excluded))
    }

    const ENGLISH: &[&str] = &[
        "correct", "core", "cored", "pass", "past", "password", "test", "testing",
    ];

    #[test]
    fn decides_once_the_shortened_form_begins_no_word() {
        assert_eq!(decisive("password", ENGLISH, &[]).as_deref(), Some("passw"));
        assert_eq!(decisive("correct", ENGLISH, &[]).as_deref(), Some("correc"));
    }

    #[test]
    fn never_decides_while_the_shortened_form_is_still_a_word() {
        assert_eq!(decisive("tesst", ENGLISH, &[]), None);
        assert_eq!(decisive("pass", ENGLISH, &[]), None);
    }

    #[test]
    fn a_word_left_out_on_purpose_blocks_every_prefix_of_it() {
        assert_eq!(decisive("password", ENGLISH, &["passwordy"]), None);
    }

    #[test]
    fn asks_whether_any_word_starts_with_a_prefix() {
        let english = words(ENGLISH);
        assert!(english.any_starts_with("pas"));
        assert!(english.any_starts_with("password"));
        assert!(!english.any_starts_with("pasw"));
        assert!(!english.any_starts_with("zzz"));
    }
}
