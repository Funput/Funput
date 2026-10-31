//! The personal dictionary: edits in `edit`, removal and its edges in `removal`,
//! the store round trip in `persistence`, and the app's inbox in `inbox`.

mod edit;
mod inbox;
mod persistence;
mod removal;

use crate::{DictionaryQuery, Edit, EditKind, EditOutcome, SuggestionConfig, SuggestionEngine};

fn engine() -> SuggestionEngine {
    SuggestionEngine::in_memory(SuggestionConfig::default())
}

fn apply(engine: &mut SuggestionEngine, kind: EditKind, word: &str) -> EditOutcome {
    engine.apply_edit(Edit::new(kind, word))
}

/// Every word the dictionary lists, most used first.
fn listed(engine: &SuggestionEngine) -> Vec<String> {
    engine
        .dictionary(&DictionaryQuery::default())
        .iter()
        .map(|entry| entry.word.to_owned())
        .collect()
}

fn blocked(engine: &SuggestionEngine) -> Vec<&str> {
    engine.blocked_words().collect()
}
