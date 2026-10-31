//! The trigram table: what typing and editing do to it in `learning`, how Predict
//! and Rerank read it in `query`, the store round trip in `persistence`, and the
//! memory it may take in `memory`. The table on its own is tested beside it, in
//! `ngram/table`.

mod learning;
mod memory;
mod persistence;
mod query;

use crate::{SuggestionConfig, SuggestionEngine};

fn engine() -> SuggestionEngine {
    SuggestionEngine::in_memory(SuggestionConfig::default())
}

/// Types `sentence` word by word, each after the one before, the way a platform
/// passes its context. `times` repeats it, each time from a fresh boundary.
fn typed(engine: &mut SuggestionEngine, sentence: &str, times: usize) {
    for _ in 0..times {
        let mut previous = None;
        for word in sentence.split(' ') {
            engine.learn_after(previous, word);
            previous = Some(word);
        }
    }
}

/// What the table holds for `older previous`, most used first.
fn trigram(engine: &SuggestionEngine, older: &str, previous: &str) -> Vec<(String, u16)> {
    let [Some(older), Some(previous)] = engine.context_slots(&[older, previous]) else {
        return Vec::new();
    };
    let Some(entry) = engine.trigrams.find(&[older, previous]) else {
        return Vec::new();
    };
    let mut followers: Vec<_> = entry
        .followers
        .iter()
        .filter_map(|slot| {
            slot.word(&engine.words)
                .map(|word| (word.text.clone(), slot.uses))
        })
        .collect();
    followers.sort_by(|left, right| right.1.cmp(&left.1).then_with(|| left.0.cmp(&right.0)));
    followers
}

/// A trigram by text: its two context words, its count and its followers.
type Trigram = (String, String, u16, Vec<(String, u16)>);

/// Every live trigram the table holds.
fn all_trigrams(engine: &SuggestionEngine) -> Vec<Trigram> {
    let text = |index: u32| engine.words[index as usize].text.clone();
    let mut all: Vec<_> = engine
        .trigrams
        .entries()
        .filter(|entry| entry.is_live(&engine.words))
        .map(|entry| {
            let (older, previous) = (text(entry.key[0].index), text(entry.key[1].index));
            let mut followers = trigram(engine, &older, &previous);
            followers.sort();
            (older, previous, entry.context_seen, followers)
        })
        .collect();
    all.sort();
    all
}

fn suggested(engine: &SuggestionEngine, context: &[&str], prefix: &str) -> Vec<String> {
    engine
        .suggest_in(context, prefix)
        .iter()
        .map(str::to_owned)
        .collect()
}
