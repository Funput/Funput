mod bigram;
mod dictionary;
mod eviction;
mod frequency;
mod learning;
mod lexicon;
mod marks;
mod persistence;
mod seed;
mod stress;

use super::*;

fn learned(engine: &mut SuggestionEngine, word: &str, uses: usize) {
    for _ in 0..uses {
        engine.learn(word);
    }
}

fn texts(engine: &SuggestionEngine, prefix: &str) -> Vec<String> {
    engine.suggest(prefix).iter().map(str::to_owned).collect()
}
