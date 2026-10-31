//! Edges against a store that keeps evicting: what a compact writes must read
//! back as exactly the edges that were live, never an edge revived onto the word
//! that took an evicted word's slot.

use std::collections::BTreeMap;

use proptest::prelude::*;
use tempfile::tempdir;

use crate::tests::bigram::followers_of;
use crate::{SuggestionConfig, SuggestionEngine};

const WORDS: [&str; 8] = ["anh", "em", "chị", "bạn", "mình", "ta", "xin", "chào"];

fn edges(engine: &SuggestionEngine) -> BTreeMap<(String, String), u16> {
    let mut edges = BTreeMap::new();
    for record in &engine.words {
        for (next, uses) in followers_of(engine, &record.text) {
            edges.insert((record.text.clone(), next), uses);
        }
    }
    edges
}

proptest! {
    #[test]
    fn a_compact_keeps_exactly_the_live_edges(
        steps in prop::collection::vec((0usize..WORDS.len(), any::<bool>()), 1..120),
    ) {
        let directory = tempdir().unwrap();
        // Half the vocabulary fits, so learning keeps handing slots to new words.
        let config = SuggestionConfig { max_words: 4, ..SuggestionConfig::default() };
        let mut engine = SuggestionEngine::open(directory.path(), config).unwrap();
        let mut previous = None;
        for (index, chained) in steps {
            let word = WORDS[index];
            engine.learn_after(previous.filter(|_| chained), word);
            previous = Some(word);
        }
        let before = edges(&engine);
        engine.compact().unwrap();
        drop(engine);
        let reopened = SuggestionEngine::open(directory.path(), config).unwrap();
        prop_assert_eq!(edges(&reopened), before);
    }
}
