//! Sorting what a word turned into, and which words of the lexicon to type.

/// What became of one word.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(super) enum Outcome {
    Typed,
    /// Plain letters, but fewer of them: a revert swallowed a key.
    LostKey,
    /// Left with a Vietnamese letter: the word read as a syllable (`been` → `bên`).
    Vietnamese,
    Other,
}

/// Sort `produced`, what the app shows for `word` once it ended.
pub(super) fn classify(word: &str, produced: &str) -> Outcome {
    if produced == word {
        Outcome::Typed
    } else if !produced.is_ascii() {
        Outcome::Vietnamese
    } else if produced.len() < word.len() {
        Outcome::LostKey
    } else {
        Outcome::Other
    }
}

/// The words of `tsv` written all in lowercase ASCII: the spellings Telex types as
/// they stand. Capitalised and accented entries are left out.
pub(super) fn words(tsv: &str) -> Vec<&str> {
    tsv.lines()
        .filter(|line| !line.starts_with('#'))
        .filter_map(|line| line.split('\t').next())
        .filter(|word| !word.is_empty() && word.bytes().all(|byte| byte.is_ascii_lowercase()))
        .collect()
}
