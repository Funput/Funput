//! What every engine word list shares, whichever rule it lists words for.
//!
//! The engine embeds sorted lists of English words that one of its rules would
//! otherwise get wrong (`doubled`, `collision`). Each list is made the same way:
//! rank the admissible `en.tsv` words, type them through the real engine under
//! [`setup::SETUPS`], apply the hand corrections in an `overrides.txt`
//! ([`overrides`]), and [`write`] the result sorted and lowercase, one word per
//! line, so the engine can binary-search it in place.

pub(crate) mod overrides;
pub(crate) mod setup;

use std::collections::{BTreeSet, HashMap};
use std::fs;
use std::io;
use std::path::Path;

use crate::forms;

/// Each listable word, lowercase, with its place in the frequency order. The first
/// spelling of a word wins, as `en.tsv` already keeps one per lowercase key.
pub(crate) fn ranks(tsv: &str) -> HashMap<String, usize> {
    let mut ranks = HashMap::new();
    let words = forms::entries(tsv).filter_map(|line| line.split('\t').next());
    for word in words.filter(|word| forms::admissible(word)) {
        let next = ranks.len();
        ranks.entry(word.to_ascii_lowercase()).or_insert(next);
    }
    ranks
}

/// Write `list` the way the engine reads it: one entry per line, each `\n`-ended.
/// A `BTreeSet` is already sorted and unique.
pub(crate) fn write(path: &Path, list: &BTreeSet<String>) -> io::Result<()> {
    let mut text = String::new();
    for entry in list {
        text.push_str(entry);
        text.push('\n');
    }
    fs::write(path, text)
}

pub(crate) fn invalid(message: String) -> io::Error {
    io::Error::new(io::ErrorKind::InvalidData, message)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn ranks_admissible_words_in_order_first_spelling_winning() {
        let ranks = ranks("# header\nThe\t0\nthe\t1\nfoo bar\t2\nPass\t3\n");
        assert_eq!(ranks.len(), 2);
        assert_eq!(ranks["the"], 0);
        assert_eq!(ranks["pass"], 1);
    }
}
