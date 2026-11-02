//! Completion: the bar while a word is typed, a letter at a time.

use funput_core::InputMethod;
use funput_suggestions::SuggestionSet;

use super::replay::{Engines, MIN_LETTERS};
use super::report::Tally;
use crate::dev::encode::encode;

/// Type `word` a letter at a time from the second letter on, and stop at the
/// first prefix whose bar holds it.
pub(super) fn complete(
    word: &str,
    context: &str,
    engines: &Engines,
    method: InputMethod,
    tally: &mut Tally,
) {
    let letters: Vec<(usize, char)> = word.char_indices().collect();
    if letters.len() <= MIN_LETTERS {
        return;
    }
    tally.completion_words += 1;
    for (typed, &(end, _)) in letters.iter().enumerate().skip(MIN_LETTERS) {
        let prefix = &word[..end];
        let bar = lowercase(engines.main.suggest_after_words(context, prefix));
        tally.bar[bar.len().min(3)] += 1;
        if bar.iter().any(|offered| offered == word) {
            tally.completion_found += 1;
            tally.completion_prefix_letters += typed;
            tally.saved += keys(word, method).saturating_sub(keys(prefix, method));
            return;
        }
    }
}

pub(super) fn first(set: SuggestionSet<'_>) -> Option<String> {
    lowercase(set).into_iter().next()
}

/// The bar's words, lowercased: lexicon words keep their own case (`iPhone`).
fn lowercase(set: SuggestionSet<'_>) -> Vec<String> {
    set.iter().map(str::to_lowercase).collect()
}

/// Keystrokes to type `text` with `method`.
pub(super) fn keys(text: &str, method: InputMethod) -> usize {
    encode(text, method).chars().count()
}
