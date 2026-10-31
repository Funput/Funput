mod bigram;
mod dictionary;
mod lexicon;
mod marks;
mod ngram;
mod persistence;
mod seed;
mod store;

use super::*;

fn learned(engine: &mut SuggestionEngine, word: &str, uses: usize) {
    for _ in 0..uses {
        engine.learn(word);
    }
}

fn texts(engine: &SuggestionEngine, prefix: &str) -> Vec<String> {
    engine.suggest(prefix).iter().map(str::to_owned).collect()
}

/// An edit, for tests outside `dictionary` that need one.
fn dictionary_apply(engine: &mut SuggestionEngine, kind: EditKind, word: &str) -> EditOutcome {
    engine.apply_edit(Edit::new(kind, word))
}
