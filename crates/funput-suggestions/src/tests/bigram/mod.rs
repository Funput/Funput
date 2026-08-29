//! What the engine remembers about which word followed which.

mod learning;
mod query;

use crate::SuggestionEngine;

/// The *living* followers of `word`, strongest first. Edges whose target has
/// been evicted are filtered out here exactly as a reader would filter them.
pub(super) fn followers_of(engine: &SuggestionEngine, word: &str) -> Vec<(String, u16)> {
    let Some(record) = engine.words.iter().find(|record| record.text == word) else {
        return Vec::new();
    };
    let mut edges: Vec<_> = record
        .followers
        .iter()
        .filter_map(|slot| {
            slot.word(&engine.words)
                .map(|next| (next.text.clone(), slot.uses))
        })
        .collect();
    edges.sort_by(|left, right| right.1.cmp(&left.1).then_with(|| left.0.cmp(&right.0)));
    edges
}

fn edge(text: &str, uses: u16) -> (String, u16) {
    (text.to_owned(), uses)
}
