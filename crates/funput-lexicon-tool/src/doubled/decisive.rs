//! The decisive prefixes: where the engine may stop waiting for the word boundary.
//!
//! A listed word gets its raw keys back when it ends. Mid-word the engine can do it
//! sooner, the moment the shortened form on screen can no longer begin any English
//! word: nothing begins with `pasw`, so `password` shows `passw` at its `w`.
//! Design: `docs/features/doubled-key-restore.md` §5.3.
//!
//! A raw-key prefix is decisive when, in every setup with eager restore on where a
//! revert has happened by then, neither the word on screen (lowercased) nor the keys
//! as a doubled-key habit means them begin an `en.tsv` word, and the prefix itself
//! begins no word the list leaves out on purpose. A screen holding a Vietnamese
//! letter begins none, which is why the habit's reading is asked as well. Setups with
//! eager restore off do not count: there the engine never switches mid-word. The
//! last rule keeps the switch from overriding the frequency call: `off` is left out
//! because `of` is the commoner word, so no prefix that `off` starts with may switch
//! to the raw keys either. Each listed word contributes its shortest decisive
//! prefix, if it has one.

use std::collections::BTreeSet;
use std::ops::Bound;

use super::scan::{self, Step};
use crate::wordlist::setup::SETUPS;

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

/// Only the setups with eager restore on count: the engine switches mid-word only
/// there, and off, every listed word waits for its boundary.
fn shortest(word: &str, english: &Words, excluded: &Words) -> Option<String> {
    let setups: Vec<Vec<Step>> = SETUPS
        .iter()
        .filter(|setup| setup.eager)
        .map(|&setup| scan::typed(word, setup))
        .collect();
    (1..=word.len()).find_map(|len| {
        let prefix = &word[..len];
        let steps = setups.iter().map(|steps| &steps[len - 1]);
        let reverted = steps.clone().any(|step| step.reverted);
        let unambiguous = steps
            .filter(|step| step.reverted)
            .all(|step| !begins_a_word(english, step, prefix));
        (reverted && unambiguous && !excluded.any_starts_with(prefix)).then(|| prefix.to_owned())
    })
}

/// Whether the word typed so far could still be the start of an English word the
/// user means. The screen is asked first; one holding a Vietnamese letter begins no
/// word (`nêcs` for `necess`). So the keys are asked too, as a user doubling a key
/// for a plain letter means them (`neces`, which begins `necessary`), whenever a
/// doubled key reverted; with none, they are just the prefix of the listed word.
fn begins_a_word(english: &Words, step: &Step, prefix: &str) -> bool {
    english.any_starts_with(&step.buffer.to_lowercase())
        || (step.meant != prefix && english.any_starts_with(&step.meant))
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
    fn a_vietnamese_screen_does_not_hide_what_a_doubled_key_meant() {
        // `necess` shows `nêcs`, which begins no word, yet a user doubling the `s`
        // means `neces`, which begins `necessa`: the switch waits one key.
        assert_eq!(
            decisive("necessa", &["necessa"], &[]).as_deref(),
            Some("necessa")
        );
        // And `necesa`, what `necessa` means to that user, may begin a word too.
        assert_eq!(decisive("necessa", &["necessa", "necesary"], &[]), None);
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
