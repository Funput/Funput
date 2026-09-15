//! Which words may be offered, how each is spelled, and in what order.
//!
//! Pure functions over already-read lines, so the whole policy is testable
//! without touching a file.

use std::cmp::Reverse;
use std::collections::{HashMap, HashSet};

use crate::{MAX_LEN, MIN_LEN};

/// Lowercase key → the one spelling offered for it.
pub(crate) type Forms = HashMap<String, String>;

/// Whether the lexicon can hold `word` at all: plain ASCII letters, two to 32 of
/// them. Everything else — possessives, contractions, dotted abbreviations,
/// accented loanwords, hyphenated compounds — is out of scope for now.
pub(crate) fn admissible(word: &str) -> bool {
    (MIN_LEN..=MAX_LEN).contains(&word.len()) && word.bytes().all(|byte| byte.is_ascii_alphabetic())
}

/// One spelling per lowercase key. SCOWL can list several (`polish` and
/// `Polish`, `us` and `US`); the lowercase one wins, then a capitalised one,
/// then the rest (`iPhone`, `USA`), ties broken by byte order so a rebuild never
/// reorders the file.
pub(crate) fn canonical<'a>(words: impl IntoIterator<Item = &'a str>) -> Forms {
    let mut forms = Forms::new();
    for word in words
        .into_iter()
        .map(str::trim)
        .filter(|word| admissible(word))
    {
        let key = word.to_ascii_lowercase();
        let better = forms
            .get(&key)
            .is_none_or(|held| (shape(word), word) < (shape(held), held.as_str()));
        if better {
            forms.insert(key, word.to_owned());
        }
    }
    forms
}

fn shape(word: &str) -> u8 {
    let mut bytes = word.bytes();
    if word.bytes().all(|byte| byte.is_ascii_lowercase()) {
        0
    } else if bytes.next().is_some_and(|byte| byte.is_ascii_uppercase())
        && bytes.all(|byte| byte.is_ascii_lowercase())
    {
        1
    } else {
        2
    }
}

/// The lines of a hand-kept list: blank lines and `#` comments dropped.
pub(crate) fn entries(text: &str) -> impl Iterator<Item = &str> {
    text.lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
}

/// The keys a blocklist removes. Multi-word phrases cannot match a single word
/// and are ignored.
///
/// `!word` re-admits a word an imported list blocks, so the import stays
/// verbatim and every exception shows up on its own line. A re-admit that
/// matches nothing is an error, not a no-op: it is a typo, or the import changed
/// under it.
pub(crate) fn blocked<'a>(
    lines: impl IntoIterator<Item = &'a str>,
) -> Result<HashSet<String>, String> {
    let mut blocked = HashSet::new();
    let mut readmitted = Vec::new();
    for line in lines
        .into_iter()
        .filter(|line| !line.contains(char::is_whitespace))
    {
        match line.strip_prefix('!') {
            Some(word) => readmitted.push(word.to_ascii_lowercase()),
            None => {
                blocked.insert(line.to_ascii_lowercase());
            }
        }
    }
    for word in readmitted {
        if !blocked.remove(&word) {
            return Err(format!("!{word} re-admits a word nothing blocks"));
        }
    }
    Ok(blocked)
}

/// The offered spellings with their counts, most frequent first, at most `top`
/// of them (`0` keeps all).
///
/// `floors` maps a key to the rank it must reach at least. Books undercount some
/// words — the tokenizer splits `cannot` into `can not`, and nothing from after
/// 2019 is in them at all — so a floor takes the count of whichever word holds
/// that rank in the corpus order and lifts the key to it. Words with neither a
/// count nor a floor are dropped: there is nothing to place them by.
pub(crate) fn ranked(
    forms: &Forms,
    counts: &HashMap<String, u64>,
    blocked: &HashSet<String>,
    floors: &HashMap<String, usize>,
    top: usize,
) -> Vec<(String, u64)> {
    let corpus = order(forms, blocked, |key| counts.get(key).copied());
    let lifted: HashMap<&str, u64> = floors
        .iter()
        .filter_map(|(key, rank)| Some((key.as_str(), corpus.get(*rank).or(corpus.last())?.2)))
        .collect();
    let ranked = order(forms, blocked, |key| {
        let own = counts.get(key).copied();
        own.max(lifted.get(key).copied())
    });
    let keep = if top == 0 { ranked.len() } else { top };
    ranked
        .into_iter()
        .take(keep)
        .map(|(_, form, count)| (form.to_owned(), count))
        .collect()
}

/// Every unblocked form that `count` places, by count then key.
fn order<'a>(
    forms: &'a Forms,
    blocked: &HashSet<String>,
    count: impl Fn(&str) -> Option<u64>,
) -> Vec<(&'a str, &'a str, u64)> {
    let mut order: Vec<_> = forms
        .iter()
        .filter(|(key, _)| !blocked.contains(*key))
        .filter_map(|(key, form)| Some((key.as_str(), form.as_str(), count(key)?)))
        .collect();
    order.sort_unstable_by_key(|(key, _, count)| (Reverse(*count), *key));
    order
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn admits_only_plain_letters_within_length() {
        assert!(admissible("iPhone"));
        assert!(!admissible("a"));
        assert!(!admissible("don't"));
        assert!(!admissible("Blvd."));
        assert!(!admissible("café"));
        assert!(!admissible(&"x".repeat(MAX_LEN + 1)));
    }

    #[test]
    fn prefers_lowercase_then_capitalised_then_the_rest() {
        let forms = canonical(["US", "us", "Polish", "POLISH", "iPhone", "IPHONE"]);
        assert_eq!(forms["us"], "us");
        assert_eq!(forms["polish"], "Polish");
        assert_eq!(forms["iphone"], "IPHONE");
    }

    #[test]
    fn ranks_by_count_then_key_and_honours_the_blocklist() {
        let forms = canonical(["the", "then", "They", "damn"]);
        let counts = HashMap::from([
            ("the".to_owned(), 90),
            ("then".to_owned(), 40),
            ("they".to_owned(), 40),
            ("damn".to_owned(), 70),
        ]);
        let blocked = blocked(entries("# comment\nDamn\ntwo words\n")).unwrap();
        let top = ranked(&forms, &counts, &blocked, &HashMap::new(), 2);
        assert_eq!(top, [("the".to_owned(), 90), ("then".to_owned(), 40)]);
    }

    #[test]
    fn a_readmit_lifts_an_imported_block_and_must_match_one() {
        let blocked = blocked(entries("damn\nsex\n# re-admits\n!Sex\n")).unwrap();
        assert_eq!(blocked, HashSet::from(["damn".to_owned()]));
        assert!(super::blocked(entries("damn\n!sex\n")).is_err());
    }

    #[test]
    fn drops_words_the_corpus_never_saw() {
        let forms = canonical(["zzyzx"]);
        assert!(ranked(&forms, &HashMap::new(), &HashSet::new(), &HashMap::new(), 0).is_empty());
    }

    #[test]
    fn a_floor_lifts_a_word_to_the_count_at_its_rank() {
        let forms = canonical(["the", "of", "and", "cannot", "selfie"]);
        let counts = HashMap::from([
            ("the".to_owned(), 90),
            ("of".to_owned(), 60),
            ("and".to_owned(), 50),
            ("cannot".to_owned(), 5),
        ]);
        // `cannot` reaches rank 1's count; `selfie` has no count, and a floor past
        // the end lands it on the last word's.
        let floors = HashMap::from([("cannot".to_owned(), 1), ("selfie".to_owned(), 99)]);
        let words: Vec<String> = ranked(&forms, &counts, &HashSet::new(), &floors, 0)
            .into_iter()
            .map(|(word, _)| word)
            .collect();
        assert_eq!(words, ["the", "cannot", "of", "and", "selfie"]);
    }
}
